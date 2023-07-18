package com.binance.mgs.account.account.controller.client;

import com.binance.account.api.SubUserApi;
import com.binance.account.vo.subuser.request.CheckParentAndSubUserBindingRequest;
import com.binance.account.vo.subuser.request.QuerySubAccountFutureAccountRequest;
import com.binance.account.vo.subuser.request.UserIdReq;
import com.binance.account.vo.subuser.response.SubUserTypeResponse;
import com.binance.account.vo.user.UserVo;
import com.binance.accountsubuserquery.api.SubUserQueryApi;
import com.binance.accountsubuserquery.vo.request.QueryEsSubUserRequest;
import com.binance.master.models.APIRequest;
import com.binance.master.models.APIResponse;
import com.binance.master.utils.JsonUtils;
import com.binance.userbigdata.vo.subuser.request.GetSubUserBindsRequest;
import com.binance.userbigdata.vo.subuser.response.SubUserBindingVo;
import com.binance.account.vo.subuser.request.BindingParentSubUserEmailReq;
import com.binance.account.vo.subuser.request.BindingParentSubUserReq;
import com.binance.account.vo.subuser.request.QuerySubUserRequest;
import com.binance.account.vo.subuser.response.BindingParentSubUserEmailResp;
import com.binance.account.vo.subuser.response.SubUserInfoResp;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * subUser query service 切换
 */
@Log4j2
@Service
public class SubUserClient {
    @Autowired
    private SubUserApi subUserApi;
    @Autowired
    private SubUserQueryApi subUserQueryApi;

    @Autowired
    private com.binance.userbigdata.api.SubUserApi subUserApiV2;

    @Value("${mgs.account.subuser.query.switch:false}")
    private boolean mgsaccountsubuserquerySwitch;

    @Value("${queryservice.check.relation.by.parent.subuserids.switch:false}")
    private boolean checkRelationByParentSubUserIdsSwitch;

    @Value("${queryservice.check.relation.by.parent.subuseremail.switch:false}")
    private boolean checkRelationByParentSubUserEmailSwitch;

    @Value("${queryservice.select.subuser.info.switch:false}")
    private boolean selectSubUserInfoSwitch;

    public APIResponse<Boolean> checkRelationByParentSubUserIds(APIRequest<BindingParentSubUserReq> req) throws Exception {
        if (checkRelationByParentSubUserIdsSwitch) {
            return subUserQueryApi.checkRelationByParentSubUserIds(req);
        } else {
            return subUserApi.checkRelationByParentSubUserIds(req);
        }
    }

    public APIResponse<BindingParentSubUserEmailResp> checkRelationByParentSubUserEmail(APIRequest<BindingParentSubUserEmailReq> request) throws Exception {
        if (checkRelationByParentSubUserEmailSwitch) {
            return subUserQueryApi.checkRelationByParentSubUserEmail(request);
        } else {
            return subUserApi.checkRelationByParentSubUserEmail(request);
        }
    }
    public APIResponse<SubUserInfoResp> selectSubUserInfo(APIRequest<QuerySubUserRequest> request) throws Exception {
        if (selectSubUserInfoSwitch) {
            return subUserQueryApi.selectSubUserInfo(request);
        } else {
            return subUserApi.selectSubUserInfo(request);
        }
    }

    public APIResponse<SubUserTypeResponse> checkRelationByUserId(APIRequest<UserIdReq> request) throws Exception {
        if (mgsaccountsubuserquerySwitch) {
            return subUserQueryApi.checkRelationByUserId(request);
        } else {
            return subUserApi.checkRelationByUserId(request);
        }
    }

    public APIResponse<Long> checkRelationAndFutureAccountEnable(APIRequest<QuerySubAccountFutureAccountRequest> request) throws Exception {
        APIResponse<Long> response;
        if (mgsaccountsubuserquerySwitch) {
            log.info("account-subuser-query checkRelationAndFutureAccountEnable request: {}", JsonUtils.toJsonHasNullKey(request));
            response = subUserQueryApi.checkRelationAndFutureAccountEnable(request);
        } else {
            log.info("account checkRelationAndFutureAccountEnable request: {}", JsonUtils.toJsonHasNullKey(request));
            response = subUserApi.checkRelationAndFutureAccountEnable(request);
        }
        log.info("checkRelationAndFutureAccountEnable response: {}", JsonUtils.toJsonHasNullKey(response));

        return response;
    }

    public APIResponse<UserVo> checkParentAndSubUserBinding(APIRequest<CheckParentAndSubUserBindingRequest> request) throws Exception {
        APIResponse<UserVo> checkResponse;
        if (mgsaccountsubuserquerySwitch) {
            log.info("account-subuser-query checkParentAndSubUserBinding request: {}", JsonUtils.toJsonHasNullKey(request));
            checkResponse = subUserQueryApi.checkParentAndSubUserBinding(request);
        } else {
            log.info("account checkParentAndSubUserBinding request: {}", JsonUtils.toJsonHasNullKey(request));
            checkResponse = subUserApi.checkParentAndSubUserBinding(request);
        }
        log.info("checkParentAndSubUserBinding response: {}", JsonUtils.toJsonHasNullKey(checkResponse));
        return checkResponse;
    }

    public APIResponse<List<SubUserBindingVo>> getSubUserBindingsByParent(APIRequest<GetSubUserBindsRequest> request) throws Exception {
        APIResponse<List<SubUserBindingVo>> subUserBindingsApiResp;
        if (mgsaccountsubuserquerySwitch) {
            log.info("account-subuser-query getSubUserBindingsByParent request: {}", JsonUtils.toJsonHasNullKey(request));
            subUserBindingsApiResp = subUserQueryApi.getSubUserBindingsByParent(request);
        } else {
            log.info("user-big-data getSubUserBindingsByParent request: {}", JsonUtils.toJsonHasNullKey(request));
            subUserBindingsApiResp = subUserApiV2.getSubUserBindingsByParent(request);
        }
        log.info("getSubUserBindingsByParent response: {}", JsonUtils.toJsonHasNullKey(subUserBindingsApiResp));
        return subUserBindingsApiResp;
    }

    public APIResponse<SubUserInfoResp> selectSubUserInfoFromEs(APIRequest<QueryEsSubUserRequest> instance) throws Exception {
        return subUserQueryApi.selectSubUserInfoFromEs(instance);
    }
}
