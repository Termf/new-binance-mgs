package com.binance.mgs.account.service;

import com.alibaba.fastjson.JSON;
import com.binance.accountsubuser.api.EnterpriseRoleAccountApi;
import com.binance.accountsubuser.vo.enterprise.EnterpriseRoleAcctVo;
import com.binance.accountsubuser.vo.enterprise.EnterpriseRoleVo;
import com.binance.accountsubuser.vo.enterprise.EnterpriseUserAndRoleBindingVo;
import com.binance.accountsubuser.vo.enterprise.request.CreateEnterpriseRoleAccountReq;
import com.binance.accountsubuser.vo.enterprise.request.DeleteEnterpriseRoleAccountReq;
import com.binance.accountsubuser.vo.enterprise.request.QueryEnterpriseRoleAccountReq;
import com.binance.accountsubuser.vo.enterprise.request.QueryEnterpriseRoleReq;
import com.binance.accountsubuser.vo.enterprise.request.QueryEnterpriseUserRoleBindingReq;
import com.binance.accountsubuser.vo.enterprise.response.CreateEnterpriseRoleAccountRes;
import com.binance.master.commons.SearchResult;
import com.binance.master.models.APIResponse;
import com.binance.master.utils.CopyBeanUtils;
import com.binance.master.utils.LogMaskUtils;
import com.binance.mgs.account.account.vo.enterprise.CreateEnterpriseRoleAccountArg;
import com.binance.mgs.account.account.vo.enterprise.DeleteEnterpriseRoleAccountArg;
import com.binance.mgs.account.account.vo.enterprise.QueryEnterpriseRoleUserArg;
import com.binance.mgs.account.account.vo.enterprise.QueryEnterpriseRoleUserByIdArg;
import com.binance.platform.mgs.advice.helper.UserOperationHelper;
import com.binance.platform.mgs.annotations.UserOperation;
import com.binance.platform.mgs.business.AbstractBaseAction;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * @author dana.d
 */
@Slf4j
@Service
public class EnterpriseRoleService extends AbstractBaseAction {
    @Autowired
    private EnterpriseRoleAccountApi enterpriseRoleAccountApi;

    @UserOperation(name = "创建角色账户", logDeviceOperation = true, eventName = "createRoleUser")
    public CreateEnterpriseRoleAccountRes createEnterpriseRoleAccount(CreateEnterpriseRoleAccountArg arg, Long currentUserId, Long parentUserId) throws Exception {
        CreateEnterpriseRoleAccountReq createEnterpriseRoleAccountReq = CopyBeanUtils.fastCopy(arg, CreateEnterpriseRoleAccountReq.class);
        createEnterpriseRoleAccountReq.setParentUserId(parentUserId);
        createEnterpriseRoleAccountReq.setOperatorUserId(currentUserId);
        log.info("EnterpriseRoleController.createRoleUser call subUser, request={} ", LogMaskUtils.maskJsonString(JSON.toJSONString(createEnterpriseRoleAccountReq)));
        APIResponse<CreateEnterpriseRoleAccountRes> apiResponse = enterpriseRoleAccountApi.createEnterpriseRoleAccount(getInstance(createEnterpriseRoleAccountReq));
        log.info("EnterpriseRoleController.createRoleUser call subUser end response={}", LogMaskUtils.maskJsonString(JSON.toJSONString(apiResponse)));
        checkResponse(apiResponse);
        UserOperationHelper.log("operatorUserId", currentUserId);
        return apiResponse.getData();
    }

    @UserOperation(name = "删除角色账户", logDeviceOperation = true, eventName = "deleteRoleUser")
    public Integer deleteRoleAccount(DeleteEnterpriseRoleAccountArg arg, Long currentUserId, Long parentUserId) throws Exception {
        DeleteEnterpriseRoleAccountReq req = CopyBeanUtils.fastCopy(arg, DeleteEnterpriseRoleAccountReq.class);
        req.setParentUserId(parentUserId);
        req.setOperatorUserId(currentUserId);
        APIResponse<Integer> apiResponse = enterpriseRoleAccountApi.deleteRoleAccount(getInstance(req));
        log.info("EnterpriseRoleController.deleteUser end request={} response={}", LogMaskUtils.maskJsonString(JSON.toJSONString(req)),
                LogMaskUtils.maskJsonString(JSON.toJSONString(apiResponse)));
        UserOperationHelper.log("operatorUserId", currentUserId);
        checkResponse(apiResponse);
        return apiResponse.getData();
    }

    public SearchResult<EnterpriseRoleAcctVo> roleUserList(QueryEnterpriseRoleUserArg arg, Long parentUserId) throws Exception {
        QueryEnterpriseRoleAccountReq req = CopyBeanUtils.fastCopy(arg, QueryEnterpriseRoleAccountReq.class);
        req.setParentUserId(parentUserId);
        APIResponse<SearchResult<EnterpriseRoleAcctVo>> apiResponse = enterpriseRoleAccountApi.enterpriseAcctList(getInstance(req));
        log.info("EnterpriseRoleController.roleUserList end request={},response={} ", LogMaskUtils.maskJsonString(JSON.toJSONString(req)), apiResponse);
        checkResponse(apiResponse);
        return apiResponse.getData();
    }

    public List<EnterpriseRoleVo> roleList() throws Exception {
        QueryEnterpriseRoleReq req = new QueryEnterpriseRoleReq();
        log.info("EnterpriseRoleController.roleUserList start request={} ", LogMaskUtils.maskJsonString(JSON.toJSONString(req)));
        APIResponse<SearchResult<EnterpriseRoleVo>> apiResponse = enterpriseRoleAccountApi.baseRoleList(getInstance(req));
        log.info("EnterpriseRoleController.roleUserList end request={},response={} ", LogMaskUtils.maskJsonString(JSON.toJSONString(req)), apiResponse);
        checkResponse(apiResponse);
        return apiResponse.getData().getRows();
    }

    public SearchResult<EnterpriseUserAndRoleBindingVo> getEnterpriseUserAndRoleBinding(QueryEnterpriseRoleUserByIdArg arg,Long userId) throws Exception {
        QueryEnterpriseUserRoleBindingReq req = CopyBeanUtils.fastCopy(arg, QueryEnterpriseUserRoleBindingReq.class);
        req.setUserId(userId);
        APIResponse<SearchResult<EnterpriseUserAndRoleBindingVo>> apiResponse = enterpriseRoleAccountApi.getEnterpriseUserAndRoleBinding(getInstance(req));
        log.info("EnterpriseRoleController.getEnterpriseUserAndRoleBinding end request={},response={} ", LogMaskUtils.maskJsonString(JSON.toJSONString(req)), apiResponse);
        checkResponse(apiResponse);
        return apiResponse.getData();
    }
}
