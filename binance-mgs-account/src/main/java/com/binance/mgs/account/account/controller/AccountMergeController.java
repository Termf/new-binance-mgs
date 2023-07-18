package com.binance.mgs.account.account.controller;

import com.alibaba.fastjson.JSONObject;
import com.binance.account.api.UserInfoApi;
import com.binance.account.vo.accountmerge.request.PreCheckRequest;
import com.binance.account.vo.accountmerge.response.PreCheckResponse;
import com.binance.account.vo.security.request.UserIdRequest;
import com.binance.account.vo.user.UserInfoVo;
import com.binance.account.vo.user.ex.UserStatusEx;
import com.binance.accountshardingredis.utils.ShardingRedisCacheUtils;
import com.binance.compliance.api.UserComplianceApi;
import com.binance.compliance.vo.request.UserComplianceCheckRequest;
import com.binance.compliance.vo.response.UserComplianceCheckResponse;
import com.binance.master.utils.DateUtils;
import com.binance.master.utils.WebUtils;
import com.binance.mgs.account.account.helper.AccountDdosRedisHelper;
import com.binance.mgs.account.account.vo.accountMerge.MergeFlowArg;
import com.binance.mgs.account.advice.DDoSPreMonitor;
import com.binance.mgs.account.advice.OTPSendLimit;
import com.binance.platform.mgs.advice.helper.UserOperationHelper;
import com.binance.platform.mgs.base.helper.CommonUserDeviceHelper;
import com.binance.userbigdata.api.AccountMergeCheckApi;
import com.binance.userbigdata.vo.accountmerge.request.CheckMergeConditionsRequest;
import com.binance.userbigdata.vo.accountmerge.response.CheckMergeConditionsResponse;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.binance.account.api.AccountMergeApi;
import com.binance.account.api.UserSecurityApi;
import com.binance.account.vo.accountmerge.request.CheckVerifyCodeRequest;
import com.binance.account.vo.accountmerge.request.MergeFlowRequest;
import com.binance.account.vo.accountmerge.response.MergeAccountResponse;
import com.binance.account.vo.accountmerge.response.MergeFlowResponse;
import com.binance.account.vo.security.enums.BizSceneEnum;
import com.binance.account.vo.security.enums.MsgType;
import com.binance.account.vo.security.request.GetUserIdByEmailOrMobileRequest;
import com.binance.account.vo.security.request.SendEmailVerifyCodeRequest;
import com.binance.account.vo.security.response.GetUserIdByEmailOrMobileResponse;
import com.binance.account.vo.security.response.SendEmailVerifyCodeResponse;
import com.binance.account.vo.user.request.GetUserRequest;
import com.binance.account.vo.user.request.SendSmsAuthCodeV2Request;
import com.binance.account.vo.user.response.SendSmsAuthCodeV2Response;
import com.binance.master.error.BusinessException;
import com.binance.master.error.GeneralCode;
import com.binance.master.models.APIResponse;
import com.binance.master.utils.RedisCacheUtils;
import com.binance.mgs.account.AccountBaseAction;
import com.binance.mgs.account.account.vo.accountMerge.CheckVerifyCodeArg;
import com.binance.mgs.account.account.vo.accountMerge.GiveUpMergeArg;
import com.binance.mgs.account.account.vo.accountMerge.SendEmailVerifyCodeForMergeArg;
import com.binance.mgs.account.account.vo.accountMerge.SendMobileVerifyCodeForMergeArg;
import com.binance.mgs.account.constant.AccountMgsErrorCode;
import com.binance.mgs.account.constant.CacheConstant;
import com.binance.platform.mgs.annotations.UserOperation;
import com.binance.platform.mgs.base.vo.CommonRet;
import com.binance.platform.mgs.utils.PKGenarator;

import lombok.extern.slf4j.Slf4j;

import java.util.Date;
import java.util.concurrent.TimeUnit;

@RestController
@RequestMapping(value = "/v1/private/account")
@Slf4j
public class AccountMergeController extends AccountBaseAction {

    @Value("${account.merge.sendEmail.limit:5}")
    private int sendEmailLimit;
    @Value("${account.merge.sendMobile.limit:5}")
    private int sendMobileLimit;
    @Value("${account.merge.open.switch:true}")
    private Boolean accountMergeSwitch;
    @Value("${account.merge.forceKyc.switch:false}")
    private Boolean accountMergeForceKycSwitch;
    @Value("${account.merge.registerTime.switch:false}")
    private Boolean accountMergeRegisterTimeSwitch;
    @Value("${account.merge.registerLimitDays:7}")
    private Integer accountMergeRegisterLimitDays;
    @Value("${account.merge.productLine:MAINSITE}")
    private String accountMergeProductLine;
    @Value("${account.merge.operation:ENFORCE_KYC}")
    private String accountMergeOperation;
    
    @Autowired
    private UserSecurityApi userSecurityApi;
    @Autowired
    private AccountMergeApi accountMergeApi;
    @Autowired
    private CommonUserDeviceHelper userDeviceHelper;
    @Autowired
    private AccountMergeCheckApi accountMergeCheckApi;
    @Autowired
    private UserComplianceApi userComplianceApi;
    @Autowired
    private UserInfoApi userInfoApi;
    

    private RedisTemplate<String, Object> accountMgsRedisTemplate= AccountDdosRedisHelper.getInstance();

    @Value("${sharding.redis.migrate.merge.read.switch:false}")
    private Boolean shardingRedisMigrateMergeReadSwitch;
    @Value("${sharding.redis.migrate.merge.write.switch:false}")
    private Boolean shardingRedisMigrateMergeWriteSwitch;

    /**
     * 判断当前用户是否有权限进行合并账号操作，提前拦截一部分，免得一些不允许的账户点击发送验证码什么的
     * 
     */
    @PostMapping(value = "/merge/preCheck")
    public CommonRet<String> preCheck() throws Exception {
        final Long userId = getUserId();
        if (!accountMergeSwitch) {
            throw new BusinessException(GeneralCode.SYS_NOT_SUPPORT);
        }
        
        PreCheckRequest preCheckRequest = new PreCheckRequest();
        preCheckRequest.setUserId(userId);
        APIResponse<PreCheckResponse> apiResponse = accountMergeApi.preCheck(getInstance(preCheckRequest));
        checkResponse(apiResponse);
        CommonRet<String> ret = new CommonRet<>();
        return ret;
    }

    /**
     * 发短信验证码分为两种，1是给自己手机发；2是给user B的手机发；
     * 1给自己的手机发，走通用的2fa验证逻辑就可以，定义一个新的场景就ok；
     * 2给user B的手机发，需要mgs定制
     */

    @PostMapping(value = "/merge/sendMobileVerifyCodeForMerge")
    @UserOperation(name = "给被合并账号发送短信验证码", eventName = "sendMobileVerifyCodeForMerge",
            logDeviceOperation = true, deviceOperationNoteField = {},
            requestKeys = {"mobileCode", "mobile"}, requestKeyDisplayNames = {"区号", "手机号"},
            responseKeys = {"$.success",}, responseKeyDisplayNames = {"success"}, sensorsRequestKeys = {"dummy"})
    @DDoSPreMonitor(action = "AccountMergeController.sendMobileVerifyCodeForMerge")
    @OTPSendLimit(otpType = OTPSendLimit.OTP_TYPE_SMS)
    public CommonRet<String> sendMobileVerifyCodeForMerge(@RequestBody @Validated SendMobileVerifyCodeForMergeArg commonArg)
            throws Exception {
        final Long userId = getUserId();
        if (!accountMergeSwitch) {
            throw new BusinessException(GeneralCode.SYS_NOT_SUPPORT);
        }
        
        // 一天5次频率限制
        Long count = sendSmsCountForMerge(userId);
        Long newCount = sendSmsCountForMergeV2(userId);
        count = shardingRedisMigrateMergeReadSwitch ? newCount : count;
        if (count > sendMobileLimit) {
            throw new BusinessException(AccountMgsErrorCode.MERGE_SEND_EMAIL_OVERLIMIT);
        }
        
        // 做强制kyc校验
        if (accountMergeForceKycSwitch) {
            UserComplianceCheckRequest request = new UserComplianceCheckRequest();
            request.setUserId(getUserId());
            request.setProductLine(accountMergeProductLine);
            request.setOperation(accountMergeOperation);
            request.setFront(true);
            APIResponse<UserComplianceCheckResponse> apiResponse = userComplianceApi.userComplianceCheck(getInstance(request));
            checkResponse(apiResponse);
            UserComplianceCheckResponse userComplianceCheckResponse = apiResponse.getData();
            if(userComplianceCheckResponse != null && !userComplianceCheckResponse.isPass()) {
                log.error("sendMobileVerifyCodeForMerge error, enforce kyc block userId={} complianceResp={}", userId, JSONObject.toJSONString(userComplianceCheckResponse));
                throw new BusinessException(userComplianceCheckResponse.getErrorCode(), userComplianceCheckResponse.getErrorMessage());
            }
        }
        
        // 做用户注册时间校验
        if (accountMergeRegisterTimeSwitch) {
            com.binance.account.vo.user.request.UserIdRequest userIdRequest = new com.binance.account.vo.user.request.UserIdRequest();
            userIdRequest.setUserId(userId.toString());
            APIResponse<UserInfoVo> userInfoVoResponse = userInfoApi.getUserInfoByUserId(getInstance(userIdRequest));
            checkResponse(userInfoVoResponse);
            
            Date registerTime = userInfoVoResponse.getData().getInsertTime();
            // 7天之前注册的用户，不允许使用该功能
            if (registerTime.compareTo(DateUtils.addDays(new Date(), 0-accountMergeRegisterLimitDays)) > 0) {
                log.error("sendMobileVerifyCodeForMerge error, register time block userId={} registerTime={}", userId, registerTime);
                throw new BusinessException(AccountMgsErrorCode.OPERATION_NOT_ALLOWED_WITHIN_DAYS, new Object[] {accountMergeRegisterLimitDays});        
            }
        }

        RedisCacheUtils.set(userId.toString(), new Date().getTime(), CacheConstant.HOUR_HALF, CacheConstant.MERGE_HAS_SEND_VERIFYCODE);

        GetUserIdByEmailOrMobileRequest minorUserReq = new GetUserIdByEmailOrMobileRequest();
        minorUserReq.setMobileCode(commonArg.getMobileCode());
        minorUserReq.setMobile(commonArg.getMobile());
        APIResponse<GetUserIdByEmailOrMobileResponse> minorUserResp = userSecurityApi.getUserIdByMobileOrEmail(getInstance(minorUserReq));// user不存在直接报错了
        checkResponse(minorUserResp);
        if (minorUserResp.getData() == null || minorUserResp.getData().getUserId() == null) {
            // 如果账号不存在，前端报错提示
            throw new BusinessException(GeneralCode.USER_NOT_EXIST);    
        }
        if (userId.equals(minorUserResp.getData().getUserId())) {
            throw new BusinessException(GeneralCode.SYS_NOT_SUPPORT);    
        }
        // 如果账号B只绑定了手机、邮箱1个2fa，意味着继续走迁移流程时，账号B要被删除，需要拦截住
        UserIdRequest userIdRequest = new UserIdRequest();
        userIdRequest.setUserId(minorUserResp.getData().getUserId());
        APIResponse<UserStatusEx> minorUserStatusResp = userApi.getUserStatusByUserId(getInstance(userIdRequest));
        checkResponse(minorUserStatusResp);
        if (!minorUserStatusResp.getData().getIsUserMobile() || minorUserStatusResp.getData().getIsUserNotBindEmail()) {
            throw new BusinessException(AccountMgsErrorCode.ONE_2FA_UNFIT_MIGRATION_ERROR);    
        }
        
        String verifyCodeId = PKGenarator.getId();
        SendSmsAuthCodeV2Request sendSmsAuthCoderRequest = new SendSmsAuthCodeV2Request();
        sendSmsAuthCoderRequest.setMobileCode(commonArg.getMobileCode());
        sendSmsAuthCoderRequest.setMobile(commonArg.getMobile());
        sendSmsAuthCoderRequest.setUserId(minorUserResp.getData().getUserId());
        sendSmsAuthCoderRequest.setVerifyCodeId(verifyCodeId);
        sendSmsAuthCoderRequest.setMsgType(MsgType.TEXT);
        sendSmsAuthCoderRequest.setBizScene(BizSceneEnum.ACCOUNT_MERGE_MINOR_USER);
        sendSmsAuthCoderRequest.setUserChannel(commonArg.getUserChannel());
        APIResponse<SendSmsAuthCodeV2Response> apiResponse =
                userApi.sendSmsAuthCodeV2(getInstance(sendSmsAuthCoderRequest));
        checkResponse(apiResponse);
        CommonRet<String> ret = new CommonRet<>();
        return ret;
    }

    public Long sendSmsCountForMerge(Long userId) {
        if (shardingRedisMigrateMergeWriteSwitch) {
            return 0L;
        }
        try {
            String key = CacheConstant.MERGE_SEND_EMAIL_COUNT + userId;
            Long expire = accountMgsRedisTemplate.getExpire(key, TimeUnit.SECONDS);
            /**
             * 从redis中获取key对应的过期时间;
             * 如果该值有过期时间，就返回相应的过期时间;
             * 如果该值没有设置过期时间，就返回-1;
             * 如果没有该值，就返回-2;
             */
            if (null==expire || -1L==expire) {
                accountMgsRedisTemplate.expire(key, 1, TimeUnit.DAYS);
            }
            Long count = accountMgsRedisTemplate.opsForValue().increment(key);
            if (count == 1L) {
                accountMgsRedisTemplate.expire(key, 1, TimeUnit.DAYS);
            }
            log.info("sendSmsCountForMerge userId={} times={} count={}", userId, expire, count);
            return count;
        } catch (Exception e) {
            log.error("sendSmsCountForMerge error", e);
        }
        return 0L;
    }

    private Long sendSmsCountForMergeV2(Long userId) {
        try {
            String key = CacheConstant.ACCOUNT_MGS_MERGE_SEND_EMAIL_COUNT + userId;
            Long expire = ShardingRedisCacheUtils.getExpire(key);
            /**
             * 从redis中获取key对应的过期时间;
             * 如果该值有过期时间，就返回相应的过期时间;
             * 如果该值没有设置过期时间，就返回-1;
             * 如果没有该值，就返回-2;
             */
            if (null == expire || -1L == expire) {
                ShardingRedisCacheUtils.expire(key, 1, TimeUnit.DAYS);
            }
            Long count = ShardingRedisCacheUtils.increment(key, 1);
            if (count == 1L) {
                ShardingRedisCacheUtils.expire(key, 1, TimeUnit.DAYS);
            }
            log.info("sharding redis sendSmsCountForMerge userId={} times={} count={}", userId, expire, count);
            return count;
        } catch (Exception e) {
            log.error("sharding redis sendSmsCountForMerge error", e);
        }
        return 0L;
    }

    /**
     * User A理论上可以输入任意邮箱，所以对频率要做严格控制
     */
    @PostMapping(value = "/merge/sendEmailVerifyCodeForMerge")
    @UserOperation(name = "给被合并账号发送邮箱验证码", eventName = "sendEmailVerifyCodeForMerge",
            logDeviceOperation = true, deviceOperationNoteField = {},
            requestKeys = {"email"}, requestKeyDisplayNames = {"邮箱"},
            responseKeys = {"$.success",}, responseKeyDisplayNames = {"success"}, sensorsRequestKeys = {"email"})
    @DDoSPreMonitor(action = "AccountMergeController.sendEmailVerifyCodeForMerge")
    @OTPSendLimit(otpType = OTPSendLimit.OTP_TYPE_EMAIL)
    public CommonRet<SendEmailVerifyCodeResponse> sendEmailVerifyCodeForMerge(@RequestBody @Validated SendEmailVerifyCodeForMergeArg commonArg)
            throws Exception {
        final Long userId = getUserId();
        if (!accountMergeSwitch) {
            throw new BusinessException(GeneralCode.SYS_NOT_SUPPORT);
        }
        
        // 一天5次频率限制
        Long count = sendEmailCountForMerge(userId);
        Long newCount = sendEmailCountForMergeV2(userId);
        count = shardingRedisMigrateMergeReadSwitch ? newCount : count;
        if (count > sendEmailLimit) {
            throw new BusinessException(AccountMgsErrorCode.MERGE_SEND_EMAIL_OVERLIMIT);
        }

        // 做强制kyc校验
        if (accountMergeForceKycSwitch) {
            UserComplianceCheckRequest request = new UserComplianceCheckRequest();
            request.setUserId(getUserId());
            request.setProductLine(accountMergeProductLine);
            request.setOperation(accountMergeOperation);
            request.setFront(true);
            APIResponse<UserComplianceCheckResponse> apiResponse = userComplianceApi.userComplianceCheck(getInstance(request));
            checkResponse(apiResponse);
            UserComplianceCheckResponse userComplianceCheckResponse = apiResponse.getData();
            if(userComplianceCheckResponse != null && !userComplianceCheckResponse.isPass()) {
                log.error("sendMobileVerifyCodeForMerge error, enforce kyc block userId={} complianceResp={}", userId, JSONObject.toJSONString(userComplianceCheckResponse));
                throw new BusinessException(userComplianceCheckResponse.getErrorCode(), userComplianceCheckResponse.getErrorMessage());
            }
        }

        // 做用户注册时间校验
        if (accountMergeRegisterTimeSwitch) {
            com.binance.account.vo.user.request.UserIdRequest userIdRequest = new com.binance.account.vo.user.request.UserIdRequest();
            userIdRequest.setUserId(userId.toString());
            APIResponse<UserInfoVo> userInfoVoResponse = userInfoApi.getUserInfoByUserId(getInstance(userIdRequest));
            checkResponse(userInfoVoResponse);

            Date registerTime = userInfoVoResponse.getData().getInsertTime();
            // 7天之前注册的用户，不允许使用该功能
            if (registerTime.compareTo(DateUtils.addDays(new Date(), 0-accountMergeRegisterLimitDays)) > 0) {
                log.error("sendEmailVerifyCodeForMerge error, register time block userId={} registerTime={}", userId, registerTime);
                throw new BusinessException(AccountMgsErrorCode.OPERATION_NOT_ALLOWED_WITHIN_DAYS, new Object[] {accountMergeRegisterLimitDays});
            }
        }
        
        RedisCacheUtils.set(userId.toString(), new Date().getTime(), CacheConstant.HOUR_HALF, CacheConstant.MERGE_HAS_SEND_VERIFYCODE);
        
        final String email = commonArg.getEmail();
        GetUserRequest minorUserReq = new GetUserRequest();
        minorUserReq.setEmail(email);
        APIResponse<Long> emailApiResponse = userApi.getUserIdByEmail(getInstance(minorUserReq));
        if (!baseHelper.isOk(emailApiResponse)) {
            log.error("sendEmailVerifyCodeForMerge.getUserIdByEmail error :userId is illegal,emailApiResponse={}", emailApiResponse);
            checkResponse(emailApiResponse);
        }
        if (userId.equals(emailApiResponse.getData())) {
            throw new BusinessException(GeneralCode.SYS_NOT_SUPPORT);
        }
        // 如果账号B只绑定了手机、邮箱1个2fa，意味着继续走迁移流程时，账号B要被删除，需要拦截住
        UserIdRequest userIdRequest = new UserIdRequest();
        userIdRequest.setUserId(emailApiResponse.getData());
        APIResponse<UserStatusEx> minorUserStatusResp = userApi.getUserStatusByUserId(getInstance(userIdRequest));
        checkResponse(minorUserStatusResp);
        if (!minorUserStatusResp.getData().getIsUserMobile() || minorUserStatusResp.getData().getIsUserNotBindEmail()) {
            throw new BusinessException(AccountMgsErrorCode.ONE_2FA_UNFIT_MIGRATION_ERROR);
        }
        
        Long minorUserId = emailApiResponse.getData();
        SendEmailVerifyCodeRequest sendEmailVerifyCodeRequest = new SendEmailVerifyCodeRequest();
        sendEmailVerifyCodeRequest.setUserId(minorUserId);
        sendEmailVerifyCodeRequest.setBizScene(BizSceneEnum.ACCOUNT_MERGE_MINOR_USER);
        APIResponse<SendEmailVerifyCodeResponse> apiResponse = userSecurityApi.sendEmailVerifyCodeV2(getInstance(sendEmailVerifyCodeRequest));
        checkResponse(apiResponse);
        CommonRet<SendEmailVerifyCodeResponse> ret = new CommonRet<>();
        ret.setData(apiResponse.getData());
        return ret;
    }

    public Long sendEmailCountForMerge(Long userId) {
        if (shardingRedisMigrateMergeWriteSwitch) {
            return 0L;
        }
        try {
            String key = CacheConstant.MERGE_SEND_EMAIL_COUNT + userId;
            Long expire = accountMgsRedisTemplate.getExpire(key, TimeUnit.SECONDS);
            /**
             * 从redis中获取key对应的过期时间;
             * 如果该值有过期时间，就返回相应的过期时间;
             * 如果该值没有设置过期时间，就返回-1;
             * 如果没有该值，就返回-2;
             */
            if (null==expire || -1L==expire) {
                accountMgsRedisTemplate.expire(key, 1, TimeUnit.DAYS);
            }
            Long count = accountMgsRedisTemplate.opsForValue().increment(key);
            if (count == 1L) {
                accountMgsRedisTemplate.expire(key, 1, TimeUnit.DAYS);
            }
            log.info("sendEmailCountForMerge userId={} times={} count={}", userId, expire, count);
            return count;
        } catch (Exception e) {
            log.error("sendEmailCountForMerge error", e);
        }
        return 0L;
    }

    private Long sendEmailCountForMergeV2(Long userId) {
        try {
            String key = CacheConstant.ACCOUNT_MGS_MERGE_SEND_EMAIL_COUNT + userId;
            Long expire = ShardingRedisCacheUtils.getExpire(key);
            /**
             * 从redis中获取key对应的过期时间;
             * 如果该值有过期时间，就返回相应的过期时间;
             * 如果该值没有设置过期时间，就返回-1;
             * 如果没有该值，就返回-2;
             */
            if (null == expire || -1L == expire) {
                ShardingRedisCacheUtils.expire(key, 1, TimeUnit.DAYS);
            }
            Long count = ShardingRedisCacheUtils.increment(key, 1);
            if (count == 1L) {
                ShardingRedisCacheUtils.expire(key, 1, TimeUnit.DAYS);
            }
            log.info("sharding redis sendEmailCountForMerge userId={} times={} count={}", userId, expire, count);
            return count;
        } catch (Exception e) {
            log.error("sharding redis sendEmailCountForMerge error", e);
        }
        return 0L;
    }

    /**
     * 使用通用方法校验user A验证码，定制校验user B验证码；都通过后，生成随机唯一flowId，记录账号合并记录，返回flowId
     * flowId可记入缓存，用户后续操作时效性校验；若已过期，从验证码部分重新开始做；
     * @param arg
     * @return
     * @throws Exception
     */
    @PostMapping(value = "/merge/checkVerifyCodeAndStartMergeFlow")
    @UserOperation(eventName = "startMergeFlow", name = "开启账号合并流程", requestKeys = {"mergedEmail","mergedMobileCode","mergedMobile"}, requestKeyDisplayNames = {"被合并账号邮箱","被合并账号mobileCode","被合并账号手机"},
            responseKeys = {"$.success"}, responseKeyDisplayNames = {"success"})
    public CommonRet<MergeFlowResponse> checkVerifyCodeAndStartMergeFlow(@RequestBody @Validated CheckVerifyCodeArg arg) throws Exception {
        final Long userId = getUserId();
        if (!accountMergeSwitch) {
            throw new BusinessException(GeneralCode.SYS_NOT_SUPPORT);
        }
        
        // 拦截未发送过验证码的请求
        Long sendVerifyCodeTime = RedisCacheUtils.get(userId.toString(), Long.class, CacheConstant.MERGE_HAS_SEND_VERIFYCODE);
        if (sendVerifyCodeTime == null) {
            throw new BusinessException(AccountMgsErrorCode.MERGE_FLOW_EXPIRE);    
        }
        
        CheckVerifyCodeRequest checkVerifyCodeRequest = new CheckVerifyCodeRequest();
        BeanUtils.copyProperties(arg, checkVerifyCodeRequest);
        checkVerifyCodeRequest.setUserId(getUserId());

        APIResponse<MergeFlowResponse> apiResponse = accountMergeApi.checkVerifyCodeAndStartMergeFlow(getInstance(checkVerifyCodeRequest));
        checkResponse(apiResponse);
        if (baseHelper.isOk(apiResponse)) {
            String flowId = apiResponse.getData().getFlowId();
            UserOperationHelper.log("flowId", flowId);
            UserOperationHelper.log("mainUserId", apiResponse.getData().getMainUserId());
            UserOperationHelper.log("minorUserId", apiResponse.getData().getMinorUserId());
            ShardingRedisCacheUtils.set(flowId, flowId, CacheConstant.HOUR, CacheConstant.MERGE_ACTIVE_FLOWID);
        }
        return new CommonRet<>(apiResponse.getData());
    }

    /**
     * 入参只要flowId，user A和user B的信息从db里读
     * 如果check不过，报错返回校验失败的，比如KYC、LOAD、ASSET等
     * 如果check过，且B有手机号和邮箱，前端弹窗提示合并confirm，合并账号xxx，然后调用删除
     * 如果check过，且B只有手机或邮箱，1.user B有资产，前端提示自愿放弃confirm，然后调用删除；2.user B无资产，前端提示删除confirm，然后调用删除
     * @return
     */
    @PostMapping(value = "/merge/checkMergeConditions")
    public CommonRet<CheckMergeConditionsResponse> checkMergeConditions(@RequestBody @Validated MergeFlowArg arg) throws Exception {
        final String flowId = arg.getFlowId();
        if (!accountMergeSwitch) {
            throw new BusinessException(GeneralCode.SYS_NOT_SUPPORT);
        }
        
        String activeFlowId = ShardingRedisCacheUtils.get(flowId, String.class, CacheConstant.MERGE_ACTIVE_FLOWID);
        if (activeFlowId == null) {
            throw new BusinessException(AccountMgsErrorCode.MERGE_FLOW_EXPIRE);    
        }
        CheckMergeConditionsRequest request = new CheckMergeConditionsRequest();
        request.setFlowId(arg.getFlowId());
        request.setUserId(getUserId());
        request.setDeviceInfo(userDeviceHelper.buildDeviceInfo(WebUtils.getHttpServletRequest(), String.valueOf(getUserId()), getTrueUserEmail()));
        APIResponse<CheckMergeConditionsResponse> apiResponse = accountMergeCheckApi.checkMergeConditions(getInstance(request));
        checkResponse(apiResponse);
        return new CommonRet<>(apiResponse.getData());
    }

    /**
     * flowId目前新建后缓存一个小时，用户需要在1个小时内完成操作
     * 已经过期的flowId操作被拦截
     * @param arg
     * @return
     * @throws Exception
     */
    @PostMapping(value = "/merge/mergeAccount")
    @UserOperation(eventName = "mergeAccount", name = "账号合并", requestKeys = {"flowId"}, requestKeyDisplayNames = {"账号合并流程id"}, logDeviceOperation = true,
            responseKeys = {"$.success"}, responseKeyDisplayNames = {"success"})
    public CommonRet<MergeAccountResponse> mergeAccount(@RequestBody @Validated MergeFlowArg arg)
            throws Exception {
        if (!accountMergeSwitch) {
            throw new BusinessException(GeneralCode.SYS_NOT_SUPPORT);
        }
        
        final String flowId = arg.getFlowId();
        String activeFlowId = ShardingRedisCacheUtils.get(flowId, String.class, CacheConstant.MERGE_ACTIVE_FLOWID);
        if (activeFlowId == null) {
            throw new BusinessException(AccountMgsErrorCode.MERGE_FLOW_EXPIRE);
        }
        
        UserOperationHelper.log("deviceInfo", userDeviceHelper.buildDeviceInfo(WebUtils.getHttpServletRequest(), String.valueOf(getUserId()), getTrueUserEmail()));
        MergeFlowRequest mergeFlowRequest = new MergeFlowRequest();
        mergeFlowRequest.setFlowId(arg.getFlowId());
        mergeFlowRequest.setUserId(getUserId());
        APIResponse<MergeAccountResponse> apiResponse = accountMergeApi.mergeAccount(getInstance(mergeFlowRequest));
        checkResponse(apiResponse);
        return new CommonRet<>(apiResponse.getData());    
    }

    /**
     * 用户放弃账号合并
     * @param arg
     * @return
     * @throws Exception
     */
    @PostMapping(value = "/merge/giveUpMerge")
    public CommonRet<String> giveUpMerge(@RequestBody @Validated GiveUpMergeArg arg)
            throws Exception {
        if (!accountMergeSwitch) {
            throw new BusinessException(GeneralCode.SYS_NOT_SUPPORT);
        }
        
        final String flowId = arg.getFlowId();
        String activeFlowId = ShardingRedisCacheUtils.get(flowId, String.class, CacheConstant.MERGE_ACTIVE_FLOWID);
        if (activeFlowId == null) {
            throw new BusinessException(AccountMgsErrorCode.MERGE_FLOW_EXPIRE);
        }
        
        MergeFlowRequest request = new MergeFlowRequest();
        request.setFlowId(arg.getFlowId());
        request.setUserId(getUserId());
        APIResponse<Integer> apiResponse = accountMergeApi.giveUpByUser(getInstance(request));
        checkResponse(apiResponse);
        return new CommonRet<>();
    }
    
}
