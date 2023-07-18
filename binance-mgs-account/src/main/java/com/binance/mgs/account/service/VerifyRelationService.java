package com.binance.mgs.account.service;

import com.alibaba.fastjson.JSON;
import com.binance.account.api.SubUserApi;
import com.binance.account.api.UserApi;
import com.binance.account.api.UserInfoApi;
import com.binance.account.constant.AccountCommonConstant;
import com.binance.account.util.BitUtils;
import com.binance.account.vo.subuser.request.BindingParentSubUserEmailReq;
import com.binance.account.vo.subuser.request.BindingParentSubUserReq;
import com.binance.account.vo.subuser.request.CheckParentAndSubUserBindingRequest;
import com.binance.account.api.UserPermissionApi;
import com.binance.account.vo.security.request.GetUserStatusByUserIdRequest;
import com.binance.account.vo.subuser.request.QuerySubAccountFutureAccountRequest;
import com.binance.account.vo.subuser.response.BindingParentSubUserEmailResp;
import com.binance.account.vo.subuser.response.SubUserTypeResponse;
import com.binance.account.vo.user.UserInfoVo;
import com.binance.account.vo.user.ex.UserStatusEx;
import com.binance.account.vo.user.request.GetUserRequest;
import com.binance.account.vo.user.UserVo;
import com.binance.account.vo.user.request.UserIdRequest;
import com.binance.account.vo.user.response.GetUserResponse;
import com.binance.account.vo.user.response.ParentDataPermissionResp;
import com.binance.accountshardingredis.utils.ShardingRedisCacheUtils;
import com.binance.accountsubuser.api.ManagerSubUserApi;
import com.binance.accountsubuser.vo.managersubuser.vo.CheckManagerSubUserExistVo;
import com.binance.authcenter.enums.AuthErrorCode;
import com.binance.master.constant.Constant;
import com.binance.master.error.BusinessException;
import com.binance.master.error.GeneralCode;
import com.binance.master.models.APIRequest;
import com.binance.master.models.APIResponse;
import com.binance.mgs.account.account.helper.AccountMgsRedisHelper;
import com.binance.mgs.account.account.controller.client.SubUserClient;
import com.binance.mgs.account.constant.AccountMgsErrorCode;
import com.binance.mgs.account.constant.CacheConstant;
import com.binance.mgs.account.util.TimeOutRegexUtils;
import com.binance.platform.mgs.base.helper.BaseHelper;
import com.binance.platform.mgs.utils.CommonUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;

import java.util.Objects;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
public class VerifyRelationService {

    @Value("${verifyrelationservice.futureuid.cache.timeout:1800}")
    private Long futureUidCacheTimeout; // 缓存时间


    @Autowired
    private UserInfoApi userInfoApi;

    @Autowired
    private BaseHelper baseHelper;

    @Autowired
    private UserApi userApi;

    @Autowired
    private SubUserClient subUserClient;

    @Autowired
    private ManagerSubUserApi managerSubUserApi;

    @Autowired
    private UserPermissionApi userPermissionApi;

    @Autowired
    private TimeOutRegexUtils timeOutRegexUtils;

    public Long checkManageSubUserBindingAndGetSubUserId(String subUserEmail, boolean isCrossMarginUser) {
        Long parentUserId = baseHelper.getUserId();
        String parentEmail = baseHelper.getUserEmail();
        if (Objects.isNull(parentUserId) || StringUtils.isBlank(parentEmail)) {
            throw new BusinessException(AuthErrorCode.AC_PLEASE_RELOGIN);
        }

        if (StringUtils.isNotBlank(subUserEmail) && !subUserEmail.equalsIgnoreCase(parentEmail)) {
            if (!timeOutRegexUtils.validateEmail(subUserEmail)) {
                throw new BusinessException(GeneralCode.ILLEGAL_PARAM);
            }
        }
        UserVo userVo = checkSubUserStatusAndGetSubUser(subUserEmail);

        if (Objects.isNull(userVo) || Objects.isNull(userVo.getUserId())) {
            throw new BusinessException(AccountMgsErrorCode.USER_IS_NOT_MANAGE_SUBUSER);
        }

        if (isCrossMarginUser && BitUtils.isFalse(userVo.getStatus(), Constant.USER_IS_EXIST_MARGIN_ACCOUNT)) {
            throw new BusinessException(AccountMgsErrorCode.USER_NOT_OWN_MARGIN);
        }
        if (!isCrossMarginUser && BitUtils.isFalse(userVo.getStatus(), AccountCommonConstant.USER_IS_EXIST_ISOLATED_MARGIN_ACCOUNT)) {
            throw new BusinessException(AccountMgsErrorCode.USER_NOT_OWN_MARGIN);
        }

        checkRootUserAndManageSubUserBinding(userVo.getUserId(), parentUserId);
        return userVo.getUserId();
    }


    private void checkRootUserAndManageSubUserBinding(Long subUserId, Long parentUserId) {
        APIRequest<CheckManagerSubUserExistVo> request = new APIRequest<>();
        CheckManagerSubUserExistVo checkManagerSubUserExistVo = new CheckManagerSubUserExistVo();
        checkManagerSubUserExistVo.setManagerSubUserId(subUserId);
        checkManagerSubUserExistVo.setRootUserId(parentUserId);
        request.setBody(checkManagerSubUserExistVo);
        APIResponse<Boolean> apiResponse = managerSubUserApi.checkManagerSubUserExist(request);
        if (!baseHelper.isOk(apiResponse)) {
            log.info("response = {} ", JSON.toJSONString(apiResponse));
            throw new BusinessException(apiResponse.getCode(), baseHelper.getErrorMsg(apiResponse), apiResponse.getParams());
        }
        if (Objects.isNull(apiResponse) || Objects.isNull(apiResponse.getData())) {
            throw new BusinessException(GeneralCode.SYS_ERROR);
        }

        if (!apiResponse.getData()) {
            throw new BusinessException(AccountMgsErrorCode.USER_IS_NOT_BINDED_MANAGE_SUBUSER);
        }

    }


    /**
     * 此方法的缓存为：当前请求userId：对应futureUserId
     * 如果缓存没有命中，会调用account服务查询对应futureUserId
     * 其中有个特殊逻辑：getRealUserId(uid)，含义为如果当前请求的用户为custom login子账户(isCommonMerchantSubUser = true)，需要通过此方法换成其母账户再去查询futureUserId
     * @param uid
     * @return futureUserId
     */
    public Long fetchFutureUserId(Long uid) {
        if(uid == null){
            log.error("VerifyRelationService.fetchFutureUserId userId is null");
            throw new BusinessException(GeneralCode.USER_ILLEGAL_PARAMETER);
        }
        Long futureUid;
        String futureUidStr =ShardingRedisCacheUtils.get(String.valueOf(uid), String.class, CacheConstant.FUTURES_ID_REDIS_KEY_PREFIX_V2);
        if (StringUtils.isBlank(futureUidStr)) {
            futureUid = getFutureUserId(getRealUserId(uid));
            if (futureUid == null) {
                throw new BusinessException(AccountMgsErrorCode.FUTURE_ACCOUNT_NOT_EXISTS);
            }
            ShardingRedisCacheUtils.set(String.valueOf(uid), futureUid.toString(), futureUidCacheTimeout,CacheConstant.FUTURES_ID_REDIS_KEY_PREFIX_V2);
        } else {
            futureUid = Long.parseLong(futureUidStr);
        }
        return futureUid;
    }

    /**
     * 校验母子关系，并查询futureUserId
     * @param requestUid
     * @return futureUserId
     */
    public Long checkRelationAndFetchFutureUserId(Long requestUid) {
        Long queryUserId;
        Long currentLoginUserId = baseHelper.getUserId();
        //1. 如果当前请求的uid为空，或与登录账户相同，则直接根据当前登录账户的userId做查询
        if(requestUid == null || currentLoginUserId.equals(requestUid)){
            queryUserId = currentLoginUserId;
        } else {
            Long realParentUserId = getRealUserId(currentLoginUserId);
            //2. 如果当前请求的uid非空，且为母账户, 则直接查询母账户，无需母子关系校验
            if(requestUid.equals(realParentUserId)){
                queryUserId = realParentUserId;
            } else {
                GetUserResponse getUserResponse = getUserById(requestUid);
                UserStatusEx userStatusEx = new UserStatusEx(getUserResponse.getUser().getStatus(), getUserResponse.getUser().getStatusExtra());
                //3. 如果当前请求的uid非空，且为read_only子账户, 则直接查询其对应的母账户，无需母子关系校验
                if(userStatusEx.getIsCommonMerchantSubUser()){
                    queryUserId = getUserResponse.getUserInfo().getParent();
                } else {
                    //4. 如果当前请求的uid非空，非母账户，非read_only子账户，需母子关系校验
                    Long subUserId = checkBindRelation(getUserResponse.getUser().getEmail(),realParentUserId);
                    if (subUserId == null){
                        throw new BusinessException(GeneralCode.TWO_USER_ID_NOT_BOUND);
                    }
                    queryUserId = subUserId;
                }
            }
        }
        return fetchFutureUserId(queryUserId);
    }

    private Long getRealUserId(Long uid) {
        com.binance.account.vo.security.request.UserIdRequest userIdRequest = new com.binance.account.vo.security.request.UserIdRequest();
        userIdRequest.setUserId(uid);
        try {
            APIResponse<ParentDataPermissionResp> resp = userPermissionApi.queryParentDataPermission(APIRequest.instance(userIdRequest));
            if (resp.getData().getUserId() == null) {
                throw new BusinessException(GeneralCode.USER_NOT_EXIST);
            }
            return resp.getData().getUserId();
        } catch (BusinessException e) {
            log.error("userPermissionApi.queryParentDataPermission BusinessException.",e);
            throw e;
        } catch (Exception e) {
            log.error("userPermissionApi.queryParentDataPermission exception.",e);
            throw new BusinessException(GeneralCode.SYS_ERROR);
        }
    }

    public Long getFutureUserId(String subUserEmail){
        Long parentUserId = baseHelper.getUserId();
        String parentEmail = baseHelper.getUserEmail();
        if (Objects.isNull(parentUserId) || StringUtils.isBlank(parentEmail)) {
            throw new BusinessException(AuthErrorCode.AC_PLEASE_RELOGIN);
        }
        // 当前登录账号如果isCommonMerchantSubUser = true， 则将当前账户替换为其母账户，再做相关查询
        Pair<Long, Boolean> pair = getRealParentUserId();
        parentUserId = pair.getLeft();

        parentEmail = getRealParentEmail(parentUserId);

        Long queryUserId = parentUserId; // 没有子账号邮箱时，默认查母账号的; 参数邮箱也支持输入母账号邮箱
        if (StringUtils.isNotBlank(subUserEmail) && !subUserEmail.equalsIgnoreCase(parentEmail)) {
            if (!timeOutRegexUtils.validateEmail(subUserEmail)) {
                throw new BusinessException(GeneralCode.ILLEGAL_PARAM);
            }
            boolean isCommonMerchantSubUser = checkIsCommonMerchantSubUser(getUserIdByEmail(subUserEmail));
            if(!isCommonMerchantSubUser){
                Long subUserId = checkBindRelation(subUserEmail, parentUserId);
                if (Objects.isNull(subUserId)){
                    throw new BusinessException(GeneralCode.TWO_USER_ID_NOT_BOUND);
                }
                queryUserId = subUserId;
            } else {
                log.info("VerifyRelationService getFutureUserId, 当前子账号状态为 isCommonMerchantSubUser = {} ", isCommonMerchantSubUser);
            }
            // 如果当前子账号状态为 "isCommonMerchantSubUser"，表示该子账号拥有和其母账号相同的查询权限，可以查询母账号所查询到的数据
            // 因此 queryUserId = parentUserId
        }
        Long futureUserId = getFutureUserId(queryUserId);
        if (Objects.isNull(futureUserId)){
            throw new BusinessException(AccountMgsErrorCode.USER_NOT_OWN_FUTURE);

        }
        return futureUserId;
    }

    private String getRealParentEmail(Long parentUserId) {
        if(parentUserId != null){
            GetUserResponse getUserResponse = getUserById(parentUserId);
            if(getUserResponse.getUser() != null){
                return getUserResponse.getUser().getEmail();
            }
        }
        return null;
    }

    private Long getUserIdByEmail(String email) {
        GetUserRequest getUserReq = new GetUserRequest();
        getUserReq.setEmail(email);
        try {
            APIResponse<Long> apiResponse = userApi.getUserIdByEmail(baseHelper.getInstance(getUserReq));
            if (!baseHelper.isOk(apiResponse)) {
                log.info("getUserInfoByEmail error, response = {} ", apiResponse);
                throw new BusinessException(apiResponse.getCode(), baseHelper.getErrorMsg(apiResponse), apiResponse.getParams());
            }
            return apiResponse.getData();
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e1) {
            log.error("userApi.getUserStatusByUserIdFromReadOrWriteDb error", e1);
            throw new BusinessException(GeneralCode.SYS_ERROR);
        }
    }

    private boolean checkIsCommonMerchantSubUser(Long subUserId) {
        GetUserStatusByUserIdRequest userIdRequest = new GetUserStatusByUserIdRequest();
        userIdRequest.setUserId(subUserId);
        try {
            APIResponse<UserStatusEx> apiResponse = userApi.getUserStatusByUserIdFromReadOrWriteDb(baseHelper.getInstance(userIdRequest));
            if (!baseHelper.isOk(apiResponse)) {
                log.info("checkIsCommonMerchantSubUser error, response = {} ", apiResponse);
                throw new BusinessException(apiResponse.getCode(), baseHelper.getErrorMsg(apiResponse), apiResponse.getParams());
            }
            return apiResponse.getData().getIsCommonMerchantSubUser();
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e1){
            log.error("userApi.getUserStatusByUserIdFromReadOrWriteDb error",e1);
            throw new BusinessException(GeneralCode.SYS_ERROR);
        }
    }

    @Nullable
    private Long getFutureUserId(Long subUserId) {
        APIRequest<UserIdRequest> request = new APIRequest<>();
        UserIdRequest body = new UserIdRequest();
        body.setUserId(Long.toString(subUserId));
        request.setBody(body);
        try{
            APIResponse<UserInfoVo> resp = userInfoApi.getUserInfoByUserId(request);
            if (Objects.nonNull(resp)
                    && Objects.equals( APIResponse.Status.OK,resp.getStatus())
                    && Objects.nonNull(resp.getData())
            ){
                return resp.getData().getFutureUserId();
            }
        }catch (BusinessException e){
            throw e;
        }catch (Exception e1){
            log.error("userInfoApi.getUserInfoByUserId error",e1);
            throw new BusinessException(GeneralCode.SYS_ERROR);
        }
        return null;
    }

    private UserVo checkSubUserStatusAndGetSubUser(String subUserEmail) {
        APIRequest<GetUserRequest> request = new APIRequest<>();
        GetUserRequest body = new GetUserRequest();
        request.setBody(body);
        body.setEmail(subUserEmail);
        try {
            APIResponse<GetUserResponse> apiResponse = userApi.getUserByEmail(request);
            if (Objects.nonNull(apiResponse) && Objects.equals(APIResponse.Status.OK, apiResponse.getStatus())
                    && Objects.nonNull(apiResponse.getData())) {
                GetUserResponse getUserResponse = apiResponse.getData();

                UserVo user = getUserResponse.getUser();
                if (Objects.nonNull(user) && BitUtils.isTrue(user.getStatus(), AccountCommonConstant.USER_IS_MANAGER_SUB_USER)) {
                    return user;
                }
            }
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e1) {
            log.error("userInfoApi.checkSubUserStatusAndGetSubUserId error", e1);
            throw new BusinessException(GeneralCode.SYS_ERROR);
        }
        return null;
    }

    private Long checkBindRelation(String subUserEmail, Long parentUserId) {
        APIRequest<QuerySubAccountFutureAccountRequest> request = new APIRequest<>();
        QuerySubAccountFutureAccountRequest body = new QuerySubAccountFutureAccountRequest();
        body.setParentUserId(parentUserId);
        body.setEmail(subUserEmail);
        request.setBody(body);
        try {
            /*APIResponse<Long> response = subUserApi.checkRelationAndFutureAccountEnable(request);*/
            APIResponse<Long> response = subUserClient.checkRelationAndFutureAccountEnable(request);

            return response.getData();
        } catch (BusinessException e) {
            throw e;
        }catch (Exception e1){
            log.error("subUserApi.checkRelationAndFutureAccountEnable error",e1);
            throw new BusinessException(GeneralCode.SYS_ERROR);
        }
    }

    /**
     * 当前登录账号如果isCommonMerchantSubUser = true， 则将当前账户替换为其母账户，再做相关查询
     */
    private Pair<Long, Boolean>  getRealParentUserId() {
        GetUserResponse userResponse = this.getUserById(baseHelper.getUserId());
        if(userResponse != null && userResponse.getUser() != null ){
            UserStatusEx userStatusEx = new UserStatusEx(userResponse.getUser().getStatus(),userResponse.getUser().getStatusExtra());
            if(userStatusEx.getIsCommonMerchantSubUser()){
                return Pair.of(userResponse.getUserInfo().getParent(),true);
            }
        }
        return Pair.of(baseHelper.getUserId(),false);
    }

    public GetUserResponse getUserById(Long userId) {
        com.binance.account.vo.security.request.UserIdRequest request = new com.binance.account.vo.security.request.UserIdRequest();
        request.setUserId(userId);
        try {
            APIResponse<GetUserResponse> apiResponse = userApi.getUserById(APIRequest.instance(request));
            if (APIResponse.Status.ERROR == apiResponse.getStatus() || null == apiResponse.getData()) {
                log.error("VerifyRelationService UserApiClient.getUserById :userId=" + userId + "  error" + apiResponse.getErrorData());
                throw new BusinessException("getUserById failed");
            }
            return apiResponse.getData();
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e1) {
            log.error("subUserApi.checkRelationAndFutureAccountEnable error", e1);
            throw new BusinessException(GeneralCode.SYS_ERROR);
        }
    }

    public UserVo checkBindingAndGetSubUser(String subUserEmail) {
        Long parentUserId = baseHelper.getUserId();
        String parentEmail = baseHelper.getUserEmail();
        if (Objects.isNull(parentUserId) || StringUtils.isBlank(parentEmail)) {
            throw new BusinessException(AuthErrorCode.AC_PLEASE_RELOGIN);
        }
        if (!timeOutRegexUtils.validateEmail(subUserEmail)) {
            throw new BusinessException(GeneralCode.ILLEGAL_PARAM);
        }

        CheckParentAndSubUserBindingRequest checkParentAndSubUserBindingRequest = new CheckParentAndSubUserBindingRequest();
        checkParentAndSubUserBindingRequest.setParentUserId(parentUserId);
        checkParentAndSubUserBindingRequest.setEmail(subUserEmail);
        APIRequest<CheckParentAndSubUserBindingRequest> request = APIRequest.instance(checkParentAndSubUserBindingRequest);
        APIResponse<UserVo> response = null;
        try {
            response = subUserClient.checkParentAndSubUserBinding(request);
        } catch (BusinessException ex) {
            throw ex;
        } catch (Exception e) {
            log.error("subUserApi.checkParentAndSubUserBinding error", e);
            throw new BusinessException(GeneralCode.SYS_ERROR);
        }
        if (Objects.isNull(response) || !Objects.equals(APIResponse.Status.OK, response.getStatus())) {
            throw new BusinessException(response.getCode(), this.baseHelper.getErrorMsg(response), response.getParams());
        }
        UserVo userVo = response.getData();
        if (userVo == null) {
            throw new BusinessException(GeneralCode.NOT_SUB_USER);
        }

        return userVo;
    }

    public Long checkRelationByParentSubUserEmail(Long parentUserId, String subUserEmail) throws Exception{
        BindingParentSubUserEmailReq subUserEmailReq = new BindingParentSubUserEmailReq();
        subUserEmailReq.setParentUserId(parentUserId);
        subUserEmailReq.setSubUserEmail(subUserEmail);
        APIResponse<BindingParentSubUserEmailResp> response;
        try {
            response = subUserClient.checkRelationByParentSubUserEmail(APIRequest.instance(subUserEmailReq));
        } catch (BusinessException ex) {
            throw ex;
        } catch (Exception e) {
            log.error("subUserClient.checkRelationByParentSubUserEmail error", e);
            throw new BusinessException(GeneralCode.SYS_ERROR);
        }
        if (Objects.isNull(response) || !Objects.equals(APIResponse.Status.OK, response.getStatus())) {
            throw new BusinessException(response.getCode(), this.baseHelper.getErrorMsg(response), response.getParams());
        }
        return response.getData().getSubUserId();
    }

    public void checkParentAndSubRelation(Long parentUserId, SubUserTypeResponse.UserType parentUserType, String subUserId) throws Exception {
        if (parentUserType != SubUserTypeResponse.UserType.PARENT) {
            throw new BusinessException(GeneralCode.SUB_UER_FUNCTION_NOT_ENABLED);
        }
        if (StringUtils.isNotBlank(subUserId)&&!parentUserId.equals(Long.valueOf(subUserId))) {
            BindingParentSubUserReq subUserReq = new BindingParentSubUserReq();
            subUserReq.setParentUserId(parentUserId);
            subUserReq.setSubUserId(Long.valueOf(subUserId));
            APIResponse<Boolean> apiResponse;
            try {
                apiResponse = subUserClient.checkRelationByParentSubUserIds(APIRequest.instance(subUserReq));
            } catch (BusinessException ex) {
                throw ex;
            } catch (Exception e) {
                log.error("subUserClient.checkRelationByParentSubUserIds error", e);
                throw new BusinessException(GeneralCode.SYS_ERROR);
            }
            if (Objects.isNull(apiResponse) || !Objects.equals(APIResponse.Status.OK, apiResponse.getStatus())) {
                throw new BusinessException(apiResponse.getCode(), this.baseHelper.getErrorMsg(apiResponse), apiResponse.getParams());
            }
            if (!apiResponse.getData()) {
                throw new BusinessException(GeneralCode.TWO_USER_ID_NOT_BOUND);
            }
        }
    }
}
