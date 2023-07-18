package com.binance.mgs.account.service;

import com.binance.accountanalyze.api.security.CommonRecaptchaApi;
import com.binance.accountanalyze.vo.CommonRecaptchaChallengeRequest;
import com.binance.accountanalyze.vo.CommonRecaptchaChallengeResponse;
import com.binance.accountshardingredis.utils.ShardingRedisCacheUtils;
import com.binance.master.constant.Constant;
import com.binance.master.models.APIRequest;
import com.binance.master.models.APIResponse;
import com.binance.master.utils.JsonUtils;
import com.binance.master.utils.WebUtils;
import com.binance.mgs.account.account.helper.AccountHelper;
import com.binance.mgs.account.constant.CacheConstant;
import com.binance.mgs.account.security.vo.ChallengeInfo;
import com.binance.mgs.account.security.vo.SecurityPassChallengeArg;
import com.binance.mgs.account.security.vo.SecuritySelectChallengeArg;
import com.binance.mgs.account.security.vo.SecuritySelectChallengeRet;
import com.binance.mgs.account.util.CaptchaCheckUtil;
import com.binance.platform.common.RpcContext;
import com.binance.platform.common.TrackingUtils;
import com.binance.platform.env.EnvUtil;
import com.binance.platform.pool.threadpool.DynamicExecutor;
import io.jsonwebtoken.JwtBuilder;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.security.Key;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.Base64;
import java.util.Date;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

@Slf4j
@Service
public class SecurityTokenService {
    private static final String CHALLENGE_BIZ_ID = "issue_token";
    private static final SignatureAlgorithm TOKEN_SIGNATURE_ALGORITHM = SignatureAlgorithm.RS256;
    private static final String PRIVATE_KEY_ALGORITHM = "RSA";

    @Autowired
    private AccountHelper accountHelper;

    @Autowired
    private CommonRecaptchaApi commonRecaptchaApi;

    @Value("${security.challenge.select.account.analyze:false}")
    private boolean accountAnalyzeChallengeSelectEnabled;

    @Value("${security.challenge.select.account.analyze.timeout:1000}")
    private long accountAnalyzeChallengeSelectTimeoutMs;

    @Value("${security.challenge.default.type:bCAPTCHA2}")
    private String defaultChallengeType;

    @Value("${security.challenge.always.pass.type:random}")
    private String alwaysPassChallengeType;

    @Value("${security.challenge.session.timeout:1810}")
    private long sessionTimeOutSeconds;

    @Value("${security.challenge.token.ttl.seconds:36000}")
    private long tokenTtlSeconds;

    @Value("${security.challenge.token.issuer:https://binance.com}")
    private String tokenIssuer;

    @Value("${security.challenge.token.private.key.base64:}")
    private String tokenPrivateKeyBase64;

    @Value("${security.challenge.check.pattern:true}")
    private boolean challengeCheckPattern;

    @Autowired
    @Qualifier("accountAnalyzeSecurityChallengeExecutor")
    private DynamicExecutor accountAnalyzeSecurityChallengeExecutor;

    public SecuritySelectChallengeRet selectChallenge(SecuritySelectChallengeArg arg) {
        SecuritySelectChallengeRet result = new SecuritySelectChallengeRet();
        String selectedType = getSelectedChallengeType(arg);
        result.setChallengeType(selectedType);
        String sessionId = createSession(new ChallengeInfo(selectedType));
        result.setSessionId(sessionId);
        return result;
    }

    private String getSelectedChallengeType(SecuritySelectChallengeArg arg) {
        String challengeType = doSelectChallengeType(arg);
        if (StringUtils.isBlank(challengeType)) {
            return defaultChallengeType;
        }
        return challengeType;
    }

    private String doSelectChallengeType(SecuritySelectChallengeArg arg) {
        if (!accountAnalyzeChallengeSelectEnabled) {
            return doSelectChallengeTypeLocal(arg);
        }
        try {
            return getSelectedChallenge(arg);
        } catch (RuntimeException e) {
            log.error(String.format("Error while loading selected challenge for [%s]", arg), e);
        }
        return doSelectChallengeTypeLocal(arg);
    }

    private String getSelectedChallenge(SecuritySelectChallengeArg arg) {
        final Future<APIResponse<CommonRecaptchaChallengeResponse>> challengeResponseFuture = selectChallengeViaAPI(arg);
        try {
            final APIResponse<CommonRecaptchaChallengeResponse> response =
                    challengeResponseFuture.get(accountAnalyzeChallengeSelectTimeoutMs, TimeUnit.MILLISECONDS);
            if (response == null || response.getStatus() != APIResponse.Status.OK || response.getData() == null || StringUtils.isBlank(
                    response.getData().getChallengeType())) {
                return null;
            }
            return response.getData().getChallengeType();
        } catch (InterruptedException e) {
            log.error("Interrupted exception while loading selected challenge from account-analyze [{}]", arg, e);
            Thread.currentThread().interrupt();
        } catch (ExecutionException | TimeoutException e) {
            log.error("Error while loading selected challenge from account-analyze [{}]", arg, e);
        }
        return null;
    }

    public boolean isChallengeValid(SecurityPassChallengeArg arg) {
        final ChallengeInfo challengeInfo = loadChallenge(arg.getSessionId());
        if (challengeInfo == null) {
            log.info("No challenge found for sessionId [{}]", arg.getSessionId());
            return false;
        }
        if (!StringUtils.equals(arg.getValidateCodeType(), challengeInfo.getSelectedChallenge())) {
            log.warn("Invalid challenge for sessionId [{}] - passed [{}], but expected [{}]",
                    arg.getSessionId(), arg.getValidateCodeType(), challengeInfo.getSelectedChallenge());
            return false;
        }
        return !challengeCheckPattern ||
                isAlwaysPassChallenge(arg.getValidateCodeType()) ||
                CaptchaCheckUtil.checkCaptchaPattern(arg);
    }

    public boolean isChallengePassed(SecurityPassChallengeArg arg) {
        if (isAlwaysPassChallenge(arg.getValidateCodeType())) {
            log.debug("'Always passed' challenge [{}] passed", arg.getValidateCodeType());
            return true;
        }
        try {
            accountHelper.verifyCodeCache(arg, CHALLENGE_BIZ_ID, false);
            return true;
        } catch (RuntimeException e) {
            log.error("Error while challenge verification", e);
        }
        return false;
    }

    public String issueToken() {
        final Key privateKey = loadPrivateKey();
        if (privateKey == null) {
            return null;
        }


        final String token = buildToken(privateKey);
        log.trace("Generated token [{}]", token);
        return token;
    }

    private String buildToken(Key privateKey) {
        long now = System.currentTimeMillis();
        Date issuedAtTime = new Date(now);
        JwtBuilder jwtBuilder = Jwts.builder()
                .setIssuedAt(issuedAtTime)
                .setIssuer(tokenIssuer)
                .signWith(TOKEN_SIGNATURE_ALGORITHM, privateKey);
        if (tokenTtlSeconds > 0) {
            jwtBuilder.setExpiration(new Date(now + TimeUnit.SECONDS.toMillis(tokenTtlSeconds))).setNotBefore(issuedAtTime);
        }
        return jwtBuilder.compact();
    }

    private Key loadPrivateKey() {
        if (StringUtils.isBlank(tokenPrivateKeyBase64)) {
            log.warn("Empty token private key");
            return null;
        }
        try {
            byte[] keyBytes = Base64.getDecoder().decode(tokenPrivateKeyBase64);
            PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(keyBytes);
            KeyFactory keyFactory = KeyFactory.getInstance(PRIVATE_KEY_ALGORITHM);
            return keyFactory.generatePrivate(keySpec);
        } catch (InvalidKeySpecException | NoSuchAlgorithmException e) {
            log.error("Error while loading private key", e);
        }
        return null;
    }

    private String createSession(ChallengeInfo challengeInfo) {
        String sessionId = generateSessionId();
        saveChallengeInfo(sessionId, challengeInfo);
        return sessionId;
    }

    private void saveChallengeInfo(String sessionId, ChallengeInfo challengeInfo) {
        String jsonStr = JsonUtils.toJsonNotNullKey(challengeInfo);
        ShardingRedisCacheUtils.set(sessionCacheKey(sessionId), jsonStr, sessionTimeOutSeconds);
    }

    private boolean isAlwaysPassChallenge(String challengeType) {
        if (StringUtils.isBlank(challengeType) || StringUtils.isBlank(alwaysPassChallengeType)) {
            return false;
        }
        return challengeType.equals(alwaysPassChallengeType);
    }

    private Future<APIResponse<CommonRecaptchaChallengeResponse>> selectChallengeViaAPI(SecuritySelectChallengeArg arg) {
        final String traceId = TrackingUtils.getTrace();
        final String envFlag = StringUtils.defaultIfBlank(WebUtils.getHeader(Constant.GRAY_ENV_HEADER), EnvUtil.getEnvFlag());
        return accountAnalyzeSecurityChallengeExecutor.submit(() -> {
            TrackingUtils.saveTrace(traceId);
            RpcContext.getContext().set(Constant.GRAY_ENV_HEADER, envFlag);
            try {
                return commonRecaptchaApi.selectChallenge(APIRequest.instance(new CommonRecaptchaChallengeRequest(arg.getSupportedTypes())));
            } catch (RuntimeException e) {
                log.error("Error while selecting challenge from account-analyze API", e);
            } finally {
                TrackingUtils.clearTrace();
            }
            return null;
        });
    }

    private static String doSelectChallengeTypeLocal(SecuritySelectChallengeArg arg) {
        return CollectionUtils.isEmpty(arg.getSupportedTypes()) ? null : arg.getSupportedTypes().get(0);
    }

    private static String generateSessionId() {
        return UUID.randomUUID().toString();
    }

    private static ChallengeInfo loadChallenge(String sessionId) {
        String json = ShardingRedisCacheUtils.get(sessionCacheKey(sessionId));
        if (StringUtils.isBlank(json)) {
            return null;
        }
        try {
            return JsonUtils.toObj(json, ChallengeInfo.class);
        } catch (RuntimeException e) {
            log.error("Error while parsing challengeInfo json", e);
        }
        return null;
    }

    private static String sessionCacheKey(String sessionId) {
        return CacheConstant.ACCOUNT_DDOS_CHALLENGE_SESSION_ID_PREFIX + ":" + sessionId;
    }
}
