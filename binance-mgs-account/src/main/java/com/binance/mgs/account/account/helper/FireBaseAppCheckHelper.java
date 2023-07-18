package com.binance.mgs.account.account.helper;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.binance.accountshardingredis.utils.ShardingRedisCacheUtils;
import com.binance.master.enums.TerminalEnum;
import com.binance.master.error.BusinessException;
import com.binance.master.error.GeneralCode;
import com.binance.master.utils.JsonUtils;
import com.binance.master.utils.StringUtils;
import com.binance.master.utils.WebUtils;
import com.binance.mgs.account.account.vo.google.DecodedAppCheckHeader;
import com.binance.mgs.account.account.vo.google.DecodedAppCheckToken;
import com.binance.mgs.account.config.FireBaseAdminConfig;
import com.binance.mgs.account.constant.CacheConstant;
import com.binance.mgs.account.util.VersionUtil;
import com.google.common.collect.ImmutableList;
import io.jsonwebtoken.Jwts;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.binary.Base64;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.jose4j.jwk.RsaJsonWebKey;
import org.jose4j.lang.JoseException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.security.interfaces.RSAPublicKey;
import java.util.Arrays;
import java.util.List;

/**
 * @author Men Huatao (alex.men@binance.com)
 * @date 4/20/22
 **/
@Slf4j
@Component
public class FireBaseAppCheckHelper {
    public static final String JWKS_URL = "https://firebaseappcheck.googleapis.com/v1beta/jwks";
    public static final String APP_CHECK_ISSUER = "https://firebaseappcheck.googleapis.com/";
    public static final String PROJECT_ID_PREFIX = "projects/";

    @Value("${app.check.switch:false}")
    private boolean enableAppCheckSwitch;
    @Value("${app.check.android.version:100.0.0}")
    private String appCheckAndroidVersion;
    @Value("${app.check.iso.version:100.0.0}")
    private String appCheckIosVersion;

    @Autowired
    private FireBaseAdminConfig fireBaseAdminConfig;

    public Boolean verifyToken(String jwtToken) {
        if (!enableAppCheckSwitch || isLowAppVersion()) {
            return true;
        }

        if (StringUtils.isBlank(jwtToken) || "null".equals(jwtToken)) {
            return null;
        }

        try {
            String[] chunks = jwtToken.split("\\.");
            String header = new String(Base64.decodeBase64(chunks[0]));
            DecodedAppCheckHeader headers = JsonUtils.parse(header, DecodedAppCheckHeader.class);
            log.info("verify app Token headers={}", headers);
            if (!StringUtils.equals(headers.getAlg(), "RS256") || !StringUtils.equals(headers.getTyp(), "JWT")) {
                return false;
            }
            String payload = new String(Base64.decodeBase64(chunks[1]));
            DecodedAppCheckToken token = JsonUtils.parse(payload,DecodedAppCheckToken.class);
            log.info("verify app Token payload={}", token);
            TerminalEnum terminal = WebUtils.getTerminal();
            if (terminal == TerminalEnum.ANDROID) {
                List<String> auds = Arrays.asList(token.getAud());
                List<String> expectAuds = ImmutableList.of((PROJECT_ID_PREFIX + fireBaseAdminConfig.getAndroidProjectId()), PROJECT_ID_PREFIX + fireBaseAdminConfig.getAndroidProjectNumber());
                if (!auds.containsAll(expectAuds)) {
                    return false;
                }
                Jwts.parser().setSigningKey(fetchPublicKey(headers.getKid()))
                        .requireSubject(fireBaseAdminConfig.getAndroidAppId())
                        .requireIssuer(APP_CHECK_ISSUER + fireBaseAdminConfig.getAndroidProjectNumber())
                        .parseClaimsJws(jwtToken);
            }
            if (terminal == TerminalEnum.IOS) {
                List<String> auds = Arrays.asList(token.getAud());
                List<String> expectAuds = ImmutableList.of((PROJECT_ID_PREFIX + fireBaseAdminConfig.getIosProjectId()), PROJECT_ID_PREFIX + fireBaseAdminConfig.getIosProjectNumber());
                if (!auds.containsAll(expectAuds)) {
                    return false;
                }
                Jwts.parser().setSigningKey(fetchPublicKey(headers.getKid()))
                        .requireSubject(fireBaseAdminConfig.getIosAppId())
                        .requireIssuer(APP_CHECK_ISSUER + fireBaseAdminConfig.getIosProjectNumber())
                        .parseClaimsJws(jwtToken);
            }
            return true;
        } catch (Exception e) {
            log.info("verifyToken exception", e);
        }
        return false;
    }

    /**
     * 从 jwks (json web key set) 中获取公钥验签
     */
    private RSAPublicKey fetchPublicKey(String kid) throws Exception {
        String cacheResult = ShardingRedisCacheUtils.get(CacheConstant.ACCOUNT_DDOS_JWKS_KEY);
        if (StringUtils.isNotBlank(cacheResult)) {
            return fetchPublicKey(cacheResult, kid);
        }

        CloseableHttpClient httpclient = HttpClients.createDefault();
        HttpGet httpGet = new HttpGet(JWKS_URL);
        CloseableHttpResponse response = httpclient.execute(httpGet);
        log.info("fetchPublicKey kid={}", kid);
        if (response.getStatusLine().getStatusCode() == 200) {
            String result = EntityUtils.toString(response.getEntity());
            if (StringUtils.isNotBlank(result)) {
                ShardingRedisCacheUtils.set(CacheConstant.ACCOUNT_DDOS_JWKS_KEY, result, 3600 * 6); // 官方文档建议6小时
                return fetchPublicKey(result, kid);
            }
        }
        return null;
    }

    private RSAPublicKey fetchPublicKey(String jwks, String kid) throws JoseException {
        JSONObject jsonKeys = (JSONObject) JSON.parse(jwks);
        JSONArray keys = jsonKeys.getJSONArray("keys");
        for (Object o : keys) {
            JSONObject jsonObject = (JSONObject) o;
            if (jsonObject.get("kid").toString().equals(kid)) {
                return new RsaJsonWebKey(jsonObject.getInnerMap()).getRsaPublicKey();
            }
        }
        return null;
    }

    private boolean isLowAppVersion() {
        TerminalEnum terminal = WebUtils.getTerminal();
        if (terminal == null) {
            throw new BusinessException(GeneralCode.AC_VALIDATE_FAILED_REFRESH_AND_RETRY);
        }
        String currentVersion = VersionUtil.getVersion(terminal);
        log.info("app check terminal {}, currentVersion={}", terminal, currentVersion);
        switch (terminal) {
            case ANDROID:
                return StringUtils.isNotBlank(currentVersion) && VersionUtil.lower(currentVersion, appCheckAndroidVersion);
            case IOS:
                return StringUtils.isNotBlank(currentVersion) && VersionUtil.lower(currentVersion, appCheckIosVersion);
            default:
                return true;
        }
    }
}
