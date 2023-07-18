package com.binance.mgs.account.account.controller;

import com.binance.accountsubuser.api.SubUserEarnApi;
import com.binance.accountsubuser.vo.earn.EarnAllSubAccountReq;
import com.binance.accountsubuser.vo.earn.EarnAllSubAccountRes;
import com.binance.accountsubuser.vo.earn.EarnParentSummaryReq;
import com.binance.accountsubuser.vo.earn.EarnParentSummaryRes;
import com.binance.accountsubuser.vo.earn.EarnSubAccountPageQueryReq;
import com.binance.accountsubuser.vo.earn.EarnSubAccountPageQueryRes;
import com.binance.accountsubuser.vo.enums.ProfitTimeType;
import com.binance.master.models.APIRequest;
import com.binance.master.models.APIResponse;
import com.binance.mgs.account.AccountBaseAction;
import com.binance.mgs.account.account.vo.subuser.SubUserAssetInfoArg;
import com.binance.platform.mgs.base.vo.CommonRet;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author sean w
 * @date 2022/9/16
 **/
@Slf4j
@RestController
@RequestMapping(value = "/v1/private/account/subuser/earn")
public class SubUserEarnController extends AccountBaseAction {

    @Autowired
    private SubUserEarnApi subUserEarnApi;

    @ApiOperation(value = "母账户Earn汇总")
    @PostMapping(value = "/parent-user-earn/summary")
    public CommonRet<EarnParentSummaryRes> getParentEarnSummary() throws Exception {

        Long parentUserId = checkAndGetUserId();
        EarnParentSummaryReq summaryReq = new EarnParentSummaryReq();
        summaryReq.setParentUserId(parentUserId);
        summaryReq.setProfitTimeType(ProfitTimeType.LAST_THIRTY_DAYS.getProfitTimeType());
        APIResponse<EarnParentSummaryRes> apiResponse = subUserEarnApi.parentUserEarnBtc(APIRequest.instance(summaryReq));
        checkResponse(apiResponse);

        CommonRet<EarnParentSummaryRes> commonRet = new CommonRet<>();
        commonRet.setData(apiResponse.getData());
        return commonRet;
    }

    @ApiOperation(value = "子账户Earn汇总")
    @PostMapping(value = "/sub-user-earn/summary")
    public CommonRet<EarnAllSubAccountRes> getSubUserEarnSummary() throws Exception {

        Long parentUserId = checkAndGetUserId();
        EarnAllSubAccountReq allSubAccountReq = new EarnAllSubAccountReq();
        allSubAccountReq.setParentUserId(parentUserId);
        allSubAccountReq.setProfitTimeType(ProfitTimeType.LAST_THIRTY_DAYS.getProfitTimeType());
        APIResponse<EarnAllSubAccountRes> apiResponse = subUserEarnApi.allSubUserEarnBtc(APIRequest.instance(allSubAccountReq));
        checkResponse(apiResponse);

        CommonRet<EarnAllSubAccountRes> commonRet = new CommonRet<>();
        commonRet.setData(apiResponse.getData());
        return commonRet;
    }

    @ApiOperation(value = "子账户Earn分页列表")
    @PostMapping(value = "/sub-user-earn/list")
    public CommonRet<EarnSubAccountPageQueryRes> getSubUserEarnList(@RequestBody @Validated SubUserAssetInfoArg arg) throws Exception {

        Long parentUserId = checkAndGetUserId();
        EarnSubAccountPageQueryReq pageQueryReq = new EarnSubAccountPageQueryReq();
        pageQueryReq.setParentUserId(parentUserId);
        pageQueryReq.setEmail(arg.getEmail());
        pageQueryReq.setProfitTimeType(ProfitTimeType.LAST_THIRTY_DAYS.getProfitTimeType());
        pageQueryReq.setIsSubUserEnabled(arg.getIsSubUserEnabled());
        pageQueryReq.setPage(arg.getPage());
        pageQueryReq.setRows(arg.getRows());
        APIResponse<EarnSubAccountPageQueryRes> apiResponse = subUserEarnApi.subUserEarnBtcList(APIRequest.instance(pageQueryReq));
        checkResponse(apiResponse);

        CommonRet<EarnSubAccountPageQueryRes> commonRet = new CommonRet<>();
        commonRet.setData(apiResponse.getData());
        return commonRet;
    }
}
