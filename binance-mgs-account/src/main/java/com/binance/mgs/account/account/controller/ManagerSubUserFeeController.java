package com.binance.mgs.account.account.controller;

import com.binance.account.vo.user.UserVo;
import com.binance.account.vo.user.ex.UserStatusEx;
import com.binance.accountshardingredis.utils.ShardingRedisCacheUtils;
import com.binance.accountsubuser.api.ManagerSubUserFeeApi;
import com.binance.accountsubuser.vo.managersubuser.ManagerSubFeeDailyReq;
import com.binance.accountsubuser.vo.managersubuser.ManagerSubFeeDailyResp;
import com.binance.accountsubuser.vo.managersubuser.ManagerSubFeeReq;
import com.binance.accountsubuser.vo.managersubuser.ManagerSubFeeResp;
import com.binance.accountsubuser.vo.managersubuser.ManagerSubFeeUpdateReq;
import com.binance.accountsubuser.vo.managersubuser.ManagerSubFeeUpdateResp;
import com.binance.master.error.BusinessException;
import com.binance.master.error.GeneralCode;
import com.binance.master.models.APIRequest;
import com.binance.master.models.APIResponse;
import com.binance.master.utils.JsonUtils;
import com.binance.mgs.account.AccountBaseAction;
import com.binance.mgs.account.account.vo.subuser.ManagerSubUserFeeArg;
import com.binance.mgs.account.account.vo.subuser.ManagerSubUserFeeDailyArg;
import com.binance.mgs.account.account.vo.subuser.ManagerSubUserFeeUpdateArg;
import com.binance.mgs.account.account.vo.subuser.ManagerSubUserFeeUpdateRet;
import com.binance.mgs.account.constant.CacheConstant;
import com.binance.mgs.account.service.ManagerSubUserRelatedService;
import com.binance.mgs.account.service.VerifyRelationService;
import com.binance.platform.mgs.advice.helper.UserOperationHelper;
import com.binance.platform.mgs.annotations.UserOperation;
import com.binance.platform.mgs.base.vo.CommonRet;
import io.swagger.annotations.ApiOperation;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.collections.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * @author sean w
 * @date 2022/11/3
 **/
@Log4j2
@RestController
@RequestMapping(value = "/v1/private/account/managersubuser/fee")
public class ManagerSubUserFeeController extends AccountBaseAction {

    @Autowired
    private ManagerSubUserRelatedService managerSubUserRelatedService;
    @Autowired
    private VerifyRelationService verifyRelationService;
    @Autowired
    private ManagerSubUserFeeApi managerSubUserFeeApi;

    @ApiOperation(value = "交易团队查询托管子账户管理费率")
    @PostMapping(value = "/query/managerSubFee")
    public CommonRet<ManagerSubFeeResp> getManagerSubFee(@RequestBody @Validated ManagerSubUserFeeArg arg) throws Exception {
        Long parentUserId = checkAndGetUserId();
        UserStatusEx parentStatusEx = getUserStatusByUserId(parentUserId);
        //判断有没有开启母账户功能
        if (!parentStatusEx.getIsSubUserFunctionEnabled()) {
            throw new BusinessException(GeneralCode.SYS_NOT_SUPPORT);
        }
        ManagerSubFeeReq managerSubFeeReq = new ManagerSubFeeReq();
        managerSubFeeReq.setBindParentUserId(parentUserId);
        managerSubFeeReq.setPage(arg.getPage());
        managerSubFeeReq.setLimit(arg.getLimit());
        managerSubFeeReq.setHideWithOutFeeSetting(arg.isHideWithOutFeeSetting());
        APIResponse<ManagerSubFeeResp> apiResponse = managerSubUserFeeApi.tradeTeamGetManagerSubFee(APIRequest.instance(managerSubFeeReq));
        // 安全要求, 返回账户不存在等这种信息的, 模糊错误信息
        managerSubUserRelatedService.checkApiResponse(apiResponse);
        return new CommonRet<>(apiResponse.getData());
    }

    @ApiOperation(value = "交易团队更新托管子账户管理费率")
    @PostMapping(value = "/update/managerSubFee")
    @UserOperation(name = "更新托管子账户管理费率", eventName = "updateManagerSubFee", responseKeys = {"$.success",}, responseKeyDisplayNames = {"success"}, requestKeys = {"managerSubUserIds", "parentUserId", "fee"}, requestKeyDisplayNames = {"托管子账户userIds", "交易团队uid", "费率"})
    public CommonRet<ManagerSubUserFeeUpdateRet> updateManagerSubFee(@RequestBody @Validated ManagerSubUserFeeUpdateArg arg) throws Exception {
        Long parentUserId = checkAndGetUserId();
        String lockKey = CacheConstant.ACCOUNT_MGS_MANAGER_SUR_USER_SET_FEE + ":" + parentUserId;
        boolean setFlag = ShardingRedisCacheUtils.setNX(lockKey, parentUserId.toString(), 2, TimeUnit.SECONDS);
        log.info("updateManagerSubFee lockKey:{} setFlag:{}", lockKey, setFlag);
        if (!setFlag) {
            throw new BusinessException(GeneralCode.TOO_MANY_REQUESTS);
        }
        try {
            UserStatusEx parentStatusEx = getUserStatusByUserId(parentUserId);
            //判断有没有开启母账户功能
            if (!parentStatusEx.getIsSubUserFunctionEnabled()) {
                throw new BusinessException(GeneralCode.SYS_NOT_SUPPORT);
            }
            if (CollectionUtils.isEmpty(arg.getManagerSubUserIds())) {
                log.info("parentUserId:{} update managerSubUser fee list is null", parentUserId);
                throw new BusinessException(GeneralCode.ILLEGAL_PARAM);
            }
            BigDecimal fee = new BigDecimal(arg.getFee());
            if (fee.compareTo(new BigDecimal("0.00"))<0||fee.compareTo(new BigDecimal("100.00"))>0) {
                log.info("parentUserId:{} update managerSubUser fee{} is illegal", arg.getFee(), parentUserId);
                throw new BusinessException(GeneralCode.ILLEGAL_PARAM);
            }

            //去重
            List<Long> newManagerSubUserIds = arg.getManagerSubUserIds().stream().distinct().collect(Collectors.toList());
            ManagerSubFeeUpdateReq updateReq = new ManagerSubFeeUpdateReq();
            updateReq.setParentUserId(parentUserId);
            updateReq.setManagerSubUserIds(newManagerSubUserIds);
            updateReq.setFee(fee);
            APIResponse<ManagerSubFeeUpdateResp> apiResponse = managerSubUserFeeApi.tradeTeamUpdateManagerSubFee(APIRequest.instance(updateReq));
            // 安全要求, 返回账户不存在等这种信息的, 模糊错误信息
            managerSubUserRelatedService.checkApiResponse(apiResponse);
            UserOperationHelper.log("managerSubUserIds", JsonUtils.toJsonHasNullKey(arg.getManagerSubUserIds()));
            UserOperationHelper.log("parentUserId", parentUserId);
            UserOperationHelper.log("fee", arg.getFee());
        } finally {
            ShardingRedisCacheUtils.del(lockKey);
        }
        return new CommonRet<>(new ManagerSubUserFeeUpdateRet());
    }

    @ApiOperation(value = "查询托管子账户管理费率详情")
    @PostMapping(value = "/detail/managerSubFee")
    public CommonRet<ManagerSubFeeDailyResp> getManagerSubFeeDetail(@RequestBody @Validated ManagerSubUserFeeDailyArg arg) throws Exception {
        Long parentUserId = checkAndGetUserId();
        // 校验绑定关系和所传邮箱是否是托管子账户
        UserVo subUser = verifyRelationService.checkBindingAndGetSubUser(arg.getEmail());
        UserStatusEx managerSubUserStatusEx = new UserStatusEx(subUser.getStatus(), subUser.getStatusExtra());
        if (!managerSubUserStatusEx.getIsManagerSubUser()){
            throw new BusinessException(GeneralCode.SYS_NOT_SUPPORT);
        }
        ManagerSubFeeDailyReq feeDailyReq = new ManagerSubFeeDailyReq();
        feeDailyReq.setManagerSubUserId(subUser.getUserId());
        feeDailyReq.setBindParentUserId(parentUserId);
        feeDailyReq.setPage(arg.getPage());
        feeDailyReq.setLimit(arg.getLimit());
        feeDailyReq.setStartTime(arg.getStartTime());
        feeDailyReq.setEndTime(arg.getEndTime());
        feeDailyReq.setSortByField(arg.getSortByField());
        feeDailyReq.setSortDirection(arg.getSortDirection());
        APIResponse<ManagerSubFeeDailyResp> apiResponse = managerSubUserFeeApi.getManagerSubFeeDaily(APIRequest.instance(feeDailyReq));
        // 安全要求, 返回账户不存在等这种信息的, 模糊错误信息
        managerSubUserRelatedService.checkApiResponse(apiResponse);
        return new CommonRet<>(apiResponse.getData());
    }
}
