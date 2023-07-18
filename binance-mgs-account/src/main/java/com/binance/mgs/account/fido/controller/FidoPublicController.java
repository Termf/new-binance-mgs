package com.binance.mgs.account.fido.controller;

import java.util.List;
import java.util.stream.Collectors;

import javax.annotation.Resource;
import javax.validation.Valid;

import com.binance.accountshardingredis.utils.ShardingRedisCacheUtils;
import com.binance.fido2.vo.GetAllCredentialsRequest;
import com.binance.fido2.vo.GetAllCredentialsResponse;
import com.binance.master.utils.CopyBeanUtils;
import com.binance.mgs.account.account.helper.UserDeviceHelper;
import com.binance.mgs.account.advice.AccountDefenseResource;
import com.binance.mgs.account.advice.AntiBotCaptchaValidate;
import com.binance.mgs.account.advice.DDoSPreMonitor;
import com.binance.mgs.account.constant.BizType;
import com.binance.mgs.account.constant.CacheConstant;
import com.binance.mgs.account.fido.vo.AllCredentialPublicArg;
import com.binance.mgs.account.fido.vo.UserCredentialSimplifyVo;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.alibaba.fastjson.JSON;
import com.binance.account.api.UserSecurityApi;
import com.binance.fido2.api.Fido2Api;
import com.binance.fido2.vo.StartAuthenticateRequest;
import com.binance.fido2.vo.StartAuthenticateResponse;
import com.binance.master.error.BusinessException;
import com.binance.master.error.GeneralCode;
import com.binance.master.models.APIRequest;
import com.binance.master.models.APIResponse;
import com.binance.master.utils.WebUtils;
import com.binance.mgs.account.AccountBaseAction;
import com.binance.mgs.account.account.helper.DdosCacheSeviceHelper;
import com.binance.mgs.account.authcenter.helper.TokenHelper;
import com.binance.mgs.account.constant.AccountMgsErrorCode;
import com.binance.mgs.account.fido.vo.StartAuthPublicArg;
import com.binance.platform.mgs.base.helper.BaseHelper;
import com.binance.platform.mgs.base.vo.CommonRet;

import lombok.extern.slf4j.Slf4j;

import static com.binance.mgs.account.constant.BizType.LOGIN;
import static com.binance.mgs.account.constant.BizType.REFRESH_ACCESS_TOKEN;

@RestController
@RequestMapping(value = "/v1/transient/account")
@Slf4j
public class FidoPublicController extends AccountBaseAction {
    
    @Resource
    protected BaseHelper baseHelper;

    @Resource
    private UserSecurityApi userSecurityApi;

    @Autowired
    private Fido2Api fido2Api;

    @Autowired
    private TokenHelper tokenHelper;

    @Autowired
    private DdosCacheSeviceHelper ddosCacheSeviceHelper;
    
    @Autowired
    private UserDeviceHelper userDeviceHelper;
    
    @Value("${fido.rpid:www.binance.com}")
    private String rpId;
    
    @Value("${fido.rpname:www.binance.com}")
    private String rpName;
    
    @Value("${fido.origin:www.binance.com}")
    private String origin;

    @Value("#{'${new2fa.public.check.bizScene:REFRESH_ACCESSTOKEN,LOGIN}'.split(',')}")
    private List<String> checkedBizScenesForPublicNew2fa;

    @Value("${2fa.DDos.check.switch:false}")
    private boolean twoCheckDDosCheckSwitch;

    @Value("${DDos.ip.limit.count:10}")
    private int ipLimitCount;

    @Value("${DDos.normal.ip.limit.count:10}")
    private int normalUserIpLimitCount;
    

    @PostMapping("/fido2/start-auth")
    @AntiBotCaptchaValidate(bizType = {REFRESH_ACCESS_TOKEN, LOGIN})
    @DDoSPreMonitor(action = "startAuthenticate")
    @AccountDefenseResource(name = "FidoPublicController.startAuthenticate")
    public CommonRet<StartAuthenticateResponse> startAuthenticate(@RequestBody @Valid StartAuthPublicArg arg) throws Exception {
        if (checkedBizScenesForPublicNew2fa.stream().noneMatch(bizSceneName -> bizSceneName.equalsIgnoreCase(arg.getBizScene().name()))) {
            log.warn("Unsupported bizScene:{}", arg.getBizScene());
            throw new BusinessException(AccountMgsErrorCode.BIZ_SCENE_NOT_SUPPORT);
        }

        if (StringUtils.isAllBlank(arg.getRefreshToken(), arg.getLoginFlowId())) {
            throw new BusinessException(GeneralCode.ILLEGAL_PARAM);
        }
        Long userId = null;
        if (StringUtils.isNotBlank(arg.getRefreshToken())) {
            userId = tokenHelper.checkRefreshTokenAndGetUserId(arg.getRefreshToken());
        } else if (StringUtils.isNotBlank(arg.getLoginFlowId())) {
            userId = ShardingRedisCacheUtils.get(arg.getLoginFlowId(), Long.class, CacheConstant.ACCOUNT_MGS_LOGIN_FLOWID_CACHE_KEY);
            if (userId == null) {
                throw new BusinessException(AccountMgsErrorCode.PLEASE_USE_PASSWORD_LOGIN);    
            }
        }
        if (userId == null) {
            log.error("public start-auth get userId failed");
            throw new BusinessException(GeneralCode.ILLEGAL_PARAM);
        }

//        if(StringUtils.isNotBlank(arg.getRefreshToken())){
//            String tempEmail= tokenHelper.checkRefreshTokenAndGetEmail(arg.getRefreshToken());
//            arg.setEmail(tempEmail);
//        }
//
//        // 邮箱或手机至少有一个
//        if (StringUtils.isAnyBlank(arg.getMobile(), arg.getMobileCode()) && StringUtils.isBlank(arg.getEmail())) {
//            throw new BusinessException(GeneralCode.USER_ILLEGAL_PARAMETER);
//        }
//
//        GetUserIdByEmailOrMobileRequest getUserIdReq = new GetUserIdByEmailOrMobileRequest();
//        getUserIdReq.setEmail(arg.getEmail());
//        getUserIdReq.setMobileCode(arg.getMobileCode());
//        getUserIdReq.setMobile(arg.getMobile());
//        APIResponse<GetUserIdByEmailOrMobileResponse> getUserIdResp = userSecurityApi.getUserIdByMobileOrEmail(getInstance(getUserIdReq));
//
//        String ip = WebUtils.getRequestIp();
//        if (!baseHelper.isOk(getUserIdResp)) {
//            if (twoCheckDDosCheckSwitch) {
//                String identify = Objects.nonNull(arg.getEmail()) ? String.valueOf(arg.getEmail()) : arg.getMobileCode() + "-" + arg.getMobile();
//                ddosCacheSeviceHelper.banIpIfNecessary(UserPerformanceEnum.NOT_EXIST, ip, ipLimitCount, String.format("public startAuthenticate identify=%s", identify));
//            }
//            log.warn("login:user illegal,userSecurityApi.getUserIdByMobileOrEmail,response={}", getUserIdResp);
//            checkResponseMisleaderUseNotExitsErrorForPublicInterface(getUserIdResp);
//        } else {
//            if (twoCheckDDosCheckSwitch) {
//                ddosCacheSeviceHelper.banIpIfNecessary(UserPerformanceEnum.NORMAL_REGISTER, ip, normalUserIpLimitCount, "public startAuthenticate");
//            }
//        }
//        Long userId = getUserIdResp.getData().getUserId();

        String clientId = userDeviceHelper.getBncUuid(WebUtils.getHttpServletRequest());
        StartAuthenticateRequest request = new StartAuthenticateRequest();
        request.setClientId(clientId);
        request.setClientType(this.baseHelper.getClientType());
        request.setRpId(this.rpId);
        request.setUserId(userId.toString());
        log.info("fido public start Authenticate request:{}", JSON.toJSONString(request));
        APIResponse<StartAuthenticateResponse> startAuth = this.fido2Api.startAuth(APIRequest.instance(request));
        log.info("fido public start Authenticate response:{}", JSON.toJSONString(startAuth));
        this.baseHelper.checkResponse(startAuth);
        return new CommonRet<StartAuthenticateResponse>(startAuth.getData());
    }

    @PostMapping("/fido2/all-credentials")
    @AntiBotCaptchaValidate(bizType = {BizType.REFRESH_ACCESS_TOKEN, BizType.LOGIN})
    @DDoSPreMonitor(action = "allCredentialPublic")
    @AccountDefenseResource(name = "FidoPublicController.allCredentialPublic")
    public CommonRet<List<UserCredentialSimplifyVo>> allCredentialPublic(@RequestBody @Valid AllCredentialPublicArg arg) throws Exception {
        if (StringUtils.isAllBlank(arg.getRefreshToken(), arg.getLoginFlowId())) {
            throw new BusinessException(GeneralCode.ILLEGAL_PARAM);    
        }
        Long userId = null;
        if (StringUtils.isNotBlank(arg.getRefreshToken())) {
            userId = tokenHelper.checkRefreshTokenAndGetUserId(arg.getRefreshToken());    
        } else if (StringUtils.isNotBlank(arg.getLoginFlowId())) {
            userId = ShardingRedisCacheUtils.get(arg.getLoginFlowId(), Long.class, CacheConstant.ACCOUNT_MGS_LOGIN_FLOWID_CACHE_KEY);
            if (userId == null) {
                throw new BusinessException(AccountMgsErrorCode.PLEASE_USE_PASSWORD_LOGIN);
            }
        }
        if (userId == null) {
            log.error("public allCredential get userId failed");
            throw new BusinessException(GeneralCode.ILLEGAL_PARAM);    
        }
        
        GetAllCredentialsRequest request = new GetAllCredentialsRequest();
        request.setRpId(this.rpId);
        request.setUserId(String.valueOf(userId));
        log.info("fido allCredentialPublic request:{}", JSON.toJSONString(request));
        APIResponse<List<GetAllCredentialsResponse>> allCredential = this.fido2Api.allCredential(APIRequest.instance(request));
        log.info("fido allCredentialPublic response:{}", JSON.toJSONString(allCredential));
        this.baseHelper.checkResponse(allCredential);

        List<UserCredentialSimplifyVo> resultList = allCredential.getData().stream().map(x -> {
            UserCredentialSimplifyVo userCredentialSimplifyVo = CopyBeanUtils.fastCopy(x, UserCredentialSimplifyVo.class);
            userCredentialSimplifyVo.setTransportType(getTransportType(x.getTransTypes()));
            return userCredentialSimplifyVo;
        }).collect(Collectors.toList());

        return new CommonRet<>(resultList);
    }

    private String getTransportType(List<String> transports) {
        if (CollectionUtils.isEmpty(transports)) {
            return "internal";
        }

        boolean internal = false;
        boolean passkeys = false;
        // 此处未使用List.contains是为了不区分大小写
        for (String transport : transports) {
            if (transport.equalsIgnoreCase("internal")) {
                internal = true;
            }
            if(transport.equalsIgnoreCase("passkeys")) {
                passkeys = true;
            }
        }
        if(passkeys) {
            return "passkeys";
        } else if (internal) {
            return "internal";
        }
        return "external";
    }
}
