package com.binance.mgs.account.account.controller;

import com.alibaba.fastjson.JSON;
import com.binance.account.api.SubUserApi;
import com.binance.account.api.UserRegisterApi;
import com.binance.account.api.UserSecurityApi;
import com.binance.account.vo.security.enums.BizSceneEnum;
import com.binance.account.vo.security.request.SendEmailVerifyCodeRequest;
import com.binance.account.vo.security.request.VerificationTwoV3Request;
import com.binance.account.vo.security.response.SendEmailVerifyCodeResponse;
import com.binance.account.vo.security.response.VerificationTwoV3Response;
import com.binance.account.vo.subuser.SubUserInfoVo;
import com.binance.account.vo.subuser.request.BindingParentSubUserReq;
import com.binance.account.vo.subuser.request.CreateAndActiveSubUserReq;
import com.binance.account.vo.subuser.response.CreateSubUserResp;
import com.binance.account.vo.subuser.response.SubUserInfoResp;
import com.binance.account.vo.user.UserVo;
import com.binance.account.vo.user.ex.UserStatusEx;
import com.binance.account.vo.user.request.AccountActiveUserV2Request;
import com.binance.account.vo.user.request.GetUserRequest;
import com.binance.account.vo.user.response.AccountActiveUserV2Response;
import com.binance.account.vo.user.response.GetUserResponse;
import com.binance.account.vo.userRegister.SendActiveEmailVerifyCodeRequest;
import com.binance.account.vo.userRegister.SendActiveEmailVerifyCodeResponse;
import com.binance.accountshardingredis.utils.ShardingRedisCacheUtils;
import com.binance.accountsubuser.api.FlexLineSubApi;
import com.binance.accountsubuser.vo.subuser.request.FlexLineQuerySubUserReq;
import com.binance.accountsubuser.vo.subuser.response.FlexLineQuerySubUserResp;
import com.binance.accountsubuserquery.vo.request.QueryEsSubUserRequest;
import com.binance.master.error.BusinessException;
import com.binance.master.error.GeneralCode;
import com.binance.master.models.APIRequest;
import com.binance.master.models.APIResponse;
import com.binance.master.utils.LogMaskUtils;
import com.binance.mgs.account.AccountBaseAction;
import com.binance.mgs.account.account.controller.client.SubUserClient;
import com.binance.mgs.account.account.helper.AccountHelper;
import com.binance.mgs.account.account.helper.DdosCacheSeviceHelper;
import com.binance.mgs.account.account.vo.subuser.CreateSubUserRet;
import com.binance.mgs.account.account.vo.subuser.CreateSubUserV2Arg;
import com.binance.mgs.account.account.vo.subuser.GetSubUserInfoV2Arg;
import com.binance.mgs.account.account.vo.subuser.SubUserActiveV2Arg;
import com.binance.mgs.account.account.vo.subuser.SubUserEmailArg;
import com.binance.mgs.account.account.vo.subuser.SubUserInfoRet;
import com.binance.mgs.account.account.vo.subuser.SubUserSendEmailVerifyCodeArg;
import com.binance.mgs.account.advice.OTPSendLimit;
import com.binance.mgs.account.constant.AccountMgsErrorCode;
import com.binance.mgs.account.constant.CacheConstant;
import com.binance.mgs.account.advice.DDoSPreMonitor;
import com.binance.mgs.account.util.TimeOutRegexUtils;
import com.binance.platform.mgs.advice.helper.UserOperationHelper;
import com.binance.platform.mgs.annotations.UserOperation;
import com.binance.platform.mgs.base.helper.CommonUserDeviceHelper;
import com.binance.platform.mgs.base.helper.SysConfigHelper;
import com.binance.platform.mgs.base.vo.CommonPageRet;
import com.binance.platform.mgs.base.vo.CommonRet;
import com.binance.platform.mgs.enums.MgsErrorCode;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;


/**
 * @author rudy.c
 * @date 2021-09-13 19:55
 */
@Slf4j
@RestController
@RequestMapping(value = "/v2/private/account/subuser")
public class SubUserV2Controller extends AccountBaseAction {
    @Value("${subuser.check.email.limit.count:500}")
    private int subUserCheckEmailCount;
    @Value("${sub.account.send.action.limit.count:400}")
    private int subAccountSendActionCount;
    @Value("${sub.account.ddos.expire.time:3600}")
    private int subAccountDdosExpireTime;

    @Autowired
    private SubUserApi subUserApi;
    @Autowired
    private SubUserClient subUserClient;
    @Autowired
    private UserSecurityApi userSecurityApi;
    @Autowired
    private UserRegisterApi userRegisterApi;
    @Autowired
    private AccountHelper accountHelper;
    @Autowired
    private CommonUserDeviceHelper userDeviceHelper;
    @Autowired
    private TimeOutRegexUtils timeOutRegexUtils;
    @Autowired
    private DdosCacheSeviceHelper ddosCacheSeviceHelper;
    @Resource
    private SysConfigHelper sysConfigHelper;
    @Autowired
    private FlexLineSubApi flexLineSubApi;
    @Value("${sub.user.get.all.flexline.parent:true}")
    private Boolean querySubUserToGetAllFlexLineParentId;

    @ApiOperation("检查email是否可用")
    @PostMapping("/check-email")
    public CommonRet<Void> checkEmail(@RequestBody @Validated SubUserEmailArg arg) throws Exception {
        Long parentUserId = getUserId();
        if (null == parentUserId) {
            throw new BusinessException(GeneralCode.SYS_NOT_LOGIN);
        }
        String email = arg.getSubUserEmail();
        if (!timeOutRegexUtils.validateEmail(email)) {
            throw new BusinessException(GeneralCode.USER_EMAIL_NOT_CORRECT);
        }

        String redisKey = CacheConstant.SUB_USER_CHECK_EMAIL + ":" + parentUserId;
        Integer checkCount =  ShardingRedisCacheUtils.get(redisKey,Integer.class);
        if(checkCount == null) {
            checkCount = 0;
            ShardingRedisCacheUtils.set(redisKey, checkCount, CacheConstant.DAY);
        }
        if(checkCount >= subUserCheckEmailCount) {
            throw new BusinessException(AccountMgsErrorCode.SUB_USER_CHECK_EMAIL_EXCEED);
        }
        ShardingRedisCacheUtils.increment(redisKey,1L);

        GetUserRequest userRequest = new GetUserRequest();
        userRequest.setEmail(email);
        APIResponse<GetUserResponse> apiResponse = userApi.getUserByEmail(getInstance(userRequest));
        if(!baseHelper.isOk(apiResponse) && GeneralCode.USER_NOT_EXIST.getCode().equals(apiResponse.getCode())) {
            return new CommonRet<>();
        } else {
            throw new BusinessException(GeneralCode.USER_EMAIL_USE);
        }
    }

    @ApiOperation("发送邮箱验证码")
    @PostMapping("/send-email-verify-code")
    @OTPSendLimit(otpType = OTPSendLimit.OTP_TYPE_EMAIL)
    public CommonRet<Void> sendEmailVerifyCode(@RequestBody @Validated SubUserSendEmailVerifyCodeArg arg) throws Exception {
        Long parentUserId = getUserId();
        if (null == parentUserId) {
            throw new BusinessException(GeneralCode.SYS_NOT_LOGIN);
        }
        String email = arg.getSubUserEmail();
        if (!timeOutRegexUtils.validateEmail(email)) {
            throw new BusinessException(GeneralCode.USER_EMAIL_NOT_CORRECT);
        }

        if (ddosCacheSeviceHelper.subAccountActionCount(parentUserId, "sendEmailVerifyCode", subAccountDdosExpireTime) > subAccountSendActionCount) {
            throw new BusinessException(AccountMgsErrorCode.ACCOUNT_OVERLIMIT);
        }

        GetUserRequest userRequest = new GetUserRequest();
        userRequest.setEmail(email);
        APIResponse<GetUserResponse> apiResponse = userApi.getUserByEmail(getInstance(userRequest));
        if(baseHelper.isOk(apiResponse)) {
            // 用户存在，校验子母关系，并发送验证码邮件
            UserVo subUser = apiResponse.getData().getUser();
            Long subUserId = subUser.getUserId();
            log.info("parentUser send email verify code of subUser, parentUserId:{} subUserId:{}", parentUserId, subUserId);

            // 校验母子关系
            BindingParentSubUserReq relationCheckReq = new BindingParentSubUserReq();
            relationCheckReq.setParentUserId(parentUserId);
            relationCheckReq.setSubUserId(subUserId);
            APIResponse<Boolean> relationCheckResp = subUserClient.checkRelationByParentSubUserIds(getInstance(relationCheckReq));
            checkResponse(relationCheckResp);
            if (relationCheckResp.getData() == null || !relationCheckResp.getData()) {
                throw new BusinessException(AccountMgsErrorCode.NO_SEND_CODE_IF_USER_EXISTS);
            }

            UserStatusEx subUserStatus = new UserStatusEx(subUser.getStatus());
            // 校验子账号激活状态
            if (subUserStatus.getIsUserActive()) {
                throw new BusinessException(GeneralCode.USER_ALREADY_ACTIVATED);
            }
            // 限制 资管子账号、无邮箱子账号、broker子账号、未绑定邮箱账号
            if (subUserStatus.getIsAssetSubUser() || subUserStatus.getIsNoEmailSubUser() || subUserStatus.getIsBrokerSubUser() || subUserStatus.getIsUserNotBindEmail()){
                throw new BusinessException(GeneralCode.SYS_NOT_SUPPORT);
            }

            SendEmailVerifyCodeRequest sendEmailVerifyCodeRequest = new SendEmailVerifyCodeRequest();
            sendEmailVerifyCodeRequest.setUserId(subUserId);
            sendEmailVerifyCodeRequest.setResend(arg.getResend());
            sendEmailVerifyCodeRequest.setBizScene(BizSceneEnum.ACCOUNT_ACTIVATE);
            APIResponse<SendEmailVerifyCodeResponse> sendApiResponse = userSecurityApi.sendEmailVerifyCodeV2(getInstance(sendEmailVerifyCodeRequest));
            checkResponse(sendApiResponse);
        } else {
            if(!GeneralCode.USER_NOT_EXIST.getCode().equals(apiResponse.getCode())) {
                log.error("userApi.getUserByEmail error, response: {}", apiResponse);
                throw new BusinessException(GeneralCode.SYS_ERROR);
            }
            // 用户不存在，发送注册验证码
            SendActiveEmailVerifyCodeRequest request = new SendActiveEmailVerifyCodeRequest();
            request.setEmail(email);
            request.setBizScene(BizSceneEnum.ACCOUNT_ACTIVATE);
            request.setResend(arg.getResend());
            APIResponse<SendActiveEmailVerifyCodeResponse> sendEmailApiResponse = userRegisterApi.sendActiveEmailVerifyCode(getInstance(request));
            log.info("userRegisterApi.sendActiveEmailVerifyCode end request={} response={}", LogMaskUtils.maskJsonString(JSON.toJSONString(request)),
                    LogMaskUtils.maskJsonString(JSON.toJSONString(sendEmailApiResponse)));
            checkResponse(sendEmailApiResponse);
        }
        return new CommonRet<>();
    }

    @ApiOperation(value = "母账户创建子账户")
    @UserOperation(eventName = "createSubUser", name = "母账号创建子账号", sendToSensorData = false, sendToBigData = false,
            responseKeys = {"$.success"}, responseKeyDisplayNames = {"success"})
    @PostMapping(value = "/creation")
    @DDoSPreMonitor(action = "createSubUserByParentV2")
    public CommonRet<CreateSubUserRet> createSubUserByParentV2(@RequestBody @Validated CreateSubUserV2Arg arg, HttpServletRequest request) throws Exception {
        CommonRet<CreateSubUserRet> ret = new CommonRet<>();
        // 母账号登陆状态校验
        Long parentUserId = checkAndGetUserId();

        // 判断注册通道是否已关闭
        boolean registerOpen = Boolean.parseBoolean(sysConfigHelper.getCodeByDisplayName("register_open"));
        if (!registerOpen) {
            throw new BusinessException(MgsErrorCode.REGISTER_CLOSE);
        }

        // 校验邮箱格式
        String email = arg.getEmail();
        if (!timeOutRegexUtils.validateEmailForRegister(email)) {
            throw new BusinessException(GeneralCode.USER_EMAIL_NOT_CORRECT);
        }

        CreateAndActiveSubUserReq req = new CreateAndActiveSubUserReq();
        req.setParentUserId(parentUserId);
        req.setEmail(email);
        req.setTrackSource(accountHelper.getRegChannel(request.getParameter("ts")));
        req.setPassword(arg.getPassword());
        req.setRemark(request.getParameter("remark"));
        HashMap<String, String> deviceInfo = userDeviceHelper.buildDeviceInfo(request, null, arg.getEmail().trim());
        req.setDeviceInfo(deviceInfo);

        req.setEmailVerifyCode(arg.getEmailVerifyCode());
        req.setParentGoogleVerifyCode(arg.getParentGoogleVerifyCode());
        req.setParentMobileVerifyCode(arg.getParentMobileVerifyCode());

        // 请求新account微服务，创建账号
        log.info("SubUserController.createAndActiveSubUser start request={} ", LogMaskUtils.maskJsonString(JSON.toJSONString(req)));
        final APIResponse<CreateSubUserResp> apiResponse = subUserApi.createAndActiveSubUser(getInstance(req));
        log.info("SubUserController.createAndActiveSubUser end request={} response={}", LogMaskUtils.maskJsonString(JSON.toJSONString(req)),
                LogMaskUtils.maskJsonString(JSON.toJSONString(apiResponse)));
        checkResponse(apiResponse);
        if (baseHelper.isOk(apiResponse)) {
            UserOperationHelper.log("subUserId", apiResponse.getData().getUserId());
        }

        String identify = CacheConstant.ACCOUNT_MGS_ANTI_BOT_GET_USER_ID_KEY_PREFIX + ":" + arg.getEmail();
        ShardingRedisCacheUtils.del(identify);

        CreateSubUserRet createSubUserRet = new CreateSubUserRet();
        createSubUserRet.setUserId(apiResponse.getData().getUserId());
        createSubUserRet.setEmail(apiResponse.getData().getEmail());
        ret.setData(createSubUserRet);
        return ret;
    }

    @PostMapping(value = "/activeSubUser")
    @UserOperation(eventName = "activeSubUser", name = "母账号激活子账号", sendToSensorData = false, sendToBigData = false,
            responseKeys = {"$.success"}, responseKeyDisplayNames = {"success"})
    @DDoSPreMonitor(action = "activeSubUserV2")
    public CommonRet<String> activeSubUserV2(@RequestBody @Validated SubUserActiveV2Arg arg) throws Exception {
        Long parentUserId = checkAndGetUserId();
        log.info("parentUser active subUser, request, parentUserId:{} subUser:{}", parentUserId, arg.getSubUserId());

        // 校验母账号2fa
        VerificationTwoV3Request verificationTwoRequest = new VerificationTwoV3Request();
        verificationTwoRequest.setUserId(parentUserId);
        verificationTwoRequest.setBizScene(BizSceneEnum.SUBUSER_MANAGE);
        verificationTwoRequest.setGoogleVerifyCode(arg.getParentGoogleVerifyCode());
        verificationTwoRequest.setMobileVerifyCode(arg.getParentMobileVerifyCode());
        APIResponse<VerificationTwoV3Response> verificationApiResponse = userSecurityApi.verificationsTwoV3(getInstance(verificationTwoRequest));
        checkResponse(verificationApiResponse);

        // 校验母子关系
        BindingParentSubUserReq relationCheckReq = new BindingParentSubUserReq();
        relationCheckReq.setParentUserId(parentUserId);
        relationCheckReq.setSubUserId(arg.getSubUserId());
        APIResponse<Boolean> relationCheckResp = subUserClient.checkRelationByParentSubUserIds(getInstance(relationCheckReq));
        checkResponse(relationCheckResp);
        if (relationCheckResp.getData() == null || !relationCheckResp.getData()) {
            throw new BusinessException(GeneralCode.TWO_USER_ID_NOT_BOUND);
        }

        com.binance.account.vo.security.request.UserIdRequest userIdRequest = new com.binance.account.vo.security.request.UserIdRequest();
        userIdRequest.setUserId(arg.getSubUserId());
        APIResponse<UserStatusEx> subUserStatusResp = userApi.getUserStatusByUserId(getInstance(userIdRequest));
        checkResponseWithoutLog(relationCheckResp);
        UserStatusEx subUserStatus = subUserStatusResp.getData();
        // 校验子账号激活状态
        if (subUserStatus.getIsUserActive()) {
            throw new BusinessException(GeneralCode.USER_ALREADY_ACTIVATED);
        }
        // 限制 资管子账号、无邮箱子账号、broker子账号、未绑定邮箱账号
        if (subUserStatus.getIsAssetSubUser() || subUserStatus.getIsNoEmailSubUser() || subUserStatus.getIsBrokerSubUser() || subUserStatus.getIsUserNotBindEmail()){
            throw new BusinessException(GeneralCode.SYS_NOT_SUPPORT);
        }

        AccountActiveUserV2Request accountActiveUserRequest = new AccountActiveUserV2Request();
        accountActiveUserRequest.setUserId(arg.getSubUserId());
        accountActiveUserRequest.setEmailVerifyCode(arg.getEmailVerifyCode());
        APIResponse<AccountActiveUserV2Response> apiResponse = userApi.accountActiveV2(getInstance(accountActiveUserRequest));
        checkResponse(apiResponse);
        if (baseHelper.isOk(apiResponse)) {
            UserOperationHelper.log(ImmutableMap.of("userId", parentUserId, "subUserId", apiResponse.getData().getUserId()));
        }
        return new CommonRet<>();
    }

    @ApiOperation(value = "分页查询子账户列表")
    @PostMapping(value = "/info/list")
    public CommonPageRet<SubUserInfoRet> getSubUserInfoList(@RequestBody @Validated GetSubUserInfoV2Arg arg) throws Exception {
        //1 获取userid
        Long parentUserId = getUserId();
        if (null == parentUserId) {
            throw new BusinessException(GeneralCode.SYS_NOT_LOGIN);
        }
        //2 组装参数调用account接口
        QueryEsSubUserRequest querySubUserRequest = new QueryEsSubUserRequest();
        querySubUserRequest.setEmail(arg.getEmail());
        querySubUserRequest.setParentUserId(parentUserId);
        if (StringUtils.isNotBlank(arg.getIsSubUserEnabled())) {
            querySubUserRequest.setIsSubUserEnabled(Integer.valueOf(arg.getIsSubUserEnabled()));
        }
        querySubUserRequest.setPage(arg.getPage());
        querySubUserRequest.setRows(arg.getRows());
        querySubUserRequest.setRemark(arg.getRemark());
        log.info("SubUserV2Controller.getSubUserInfoList start request={} ", LogMaskUtils.maskJsonString(JSON.toJSONString(querySubUserRequest)));
        APIResponse<SubUserInfoResp> apiResponse = subUserClient.selectSubUserInfoFromEs(getInstance(querySubUserRequest));
        log.info("SubUserV2Controller.getSubUserInfoList end request={},response={} ", LogMaskUtils.maskJsonString(JSON.toJSONString(querySubUserRequest)), apiResponse);
        checkResponse(apiResponse);
        FlexLineQuerySubUserResp flexLineSubData = new FlexLineQuerySubUserResp();
        if(querySubUserToGetAllFlexLineParentId){
            APIResponse<List<Long>> allAvailableFlexLineParent = flexLineSubApi.getAllAvailableFlexLineParent();
            checkResponse(allAvailableFlexLineParent);
            List<Long> flexLineParentUserIds = allAvailableFlexLineParent.getData();
            if(!org.springframework.util.CollectionUtils.isEmpty(flexLineParentUserIds) && flexLineParentUserIds.contains(parentUserId)){
                FlexLineQuerySubUserReq flexLineQuerySubUserReq = new FlexLineQuerySubUserReq();
                flexLineQuerySubUserReq.setParentUserId(parentUserId);
                APIResponse<FlexLineQuerySubUserResp> queryFlexLineSub = flexLineSubApi.queryFlexLineSub(APIRequest.instance(flexLineQuerySubUserReq));
                checkResponse(queryFlexLineSub);
                flexLineSubData = queryFlexLineSub.getData();
            }
        }
        FlexLineQuerySubUserResp finalFlexLineSubData = flexLineSubData;

        //3 拼接返回值
        CommonPageRet<SubUserInfoRet> commonPageRet = new CommonPageRet<>();
        commonPageRet.setTotal(apiResponse.getData().getCount());
        List<SubUserInfoRet> subUserInfoRetList = Lists.newArrayList();
        for (SubUserInfoVo subUserInfoVo : apiResponse.getData().getResult()) {
            SubUserInfoRet subUserInfoRet = new SubUserInfoRet();
            BeanUtils.copyProperties(subUserInfoVo, subUserInfoRet);
            subUserInfoRet.setSubUserId(subUserInfoVo.getSubUserId().toString());
            if(finalFlexLineSubData.getCreditSubUserId()!=null){
                if(Objects.equals(subUserInfoVo.getSubUserId(), finalFlexLineSubData.getCreditSubUserId())){
                    subUserInfoRet.setIsFlexLineCreditUser(true);
                }
            }
            if (CollectionUtils.isNotEmpty(finalFlexLineSubData.getTradingSubUserIds())){
                if(finalFlexLineSubData.getTradingSubUserIds().contains(subUserInfoVo.getSubUserId())){
                    subUserInfoRet.setIsFlexLineTradingUser(true);
                }
            }
            subUserInfoRetList.add(subUserInfoRet);
        }
        commonPageRet.setData(subUserInfoRetList);
        return commonPageRet;
    }
}
