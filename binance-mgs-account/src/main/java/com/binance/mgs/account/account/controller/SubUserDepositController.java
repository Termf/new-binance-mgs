package com.binance.mgs.account.account.controller;

import com.alibaba.fastjson.JSON;
import com.binance.account.vo.user.UserVo;
import com.binance.capital.api.DepositApi;
import com.binance.capital.vo.deposit.request.ListDepositRequest;
import com.binance.capital.vo.deposit.response.ListDepositResponse;
import com.binance.master.commons.Page;
import com.binance.master.commons.SearchResult;
import com.binance.master.error.BusinessException;
import com.binance.master.error.GeneralCode;
import com.binance.master.models.APIResponse;
import com.binance.master.utils.CopyBeanUtils;
import com.binance.mgs.account.AccountBaseAction;
import com.binance.mgs.account.account.vo.subuser.QuerySubUserDepositListArg;
import com.binance.mgs.account.account.vo.subuser.QuerySubUserDepositListRet;
import com.binance.mgs.account.service.VerifyRelationService;
import com.binance.platform.mgs.base.vo.CommonPageRet;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;

/**
 * @author dana.d
 */
@Slf4j
@RestController
@RequestMapping("/v1/private/account/subUser/deposit")
public class SubUserDepositController extends AccountBaseAction {

    @Autowired
    private VerifyRelationService verifyRelationService;

    @Resource
    private DepositApi depositApi;

    @Value("${account.subuser.deposit.list.page.limit:30}")
    private Integer pageLimit;


    @PostMapping("/list")
    @ApiOperation(value = "查询子账户的充值记录列表")
    public CommonPageRet<QuerySubUserDepositListRet> list(@RequestBody @Validated QuerySubUserDepositListArg arg) throws Exception {
        if ((arg.getOffset() + arg.getLimit()) > pageLimit * arg.getLimit()) {
            log.error("request :{},pageLimit:{}", JSON.toJSON(arg), pageLimit);
            throw new BusinessException(GeneralCode.ILLEGAL_PARAM);
        }
        UserVo userVo = verifyRelationService.checkBindingAndGetSubUser(arg.getSubUserEmail());

        ListDepositRequest request = CopyBeanUtils.fastCopy(arg, ListDepositRequest.class);
        request.setUserId(userVo.getUserId());
        request.setNotStatus(2);

        Page page = new Page();
        page.setLimit(arg.getLimit());
        page.setOffset(arg.getOffset());
        request.setPage(page);

        APIResponse<SearchResult<ListDepositResponse>> apiResponse = depositApi.list(getInstance(request));
        checkResponse(apiResponse);

        SearchResult<ListDepositResponse> searchResult = apiResponse.getData();

        List<ListDepositResponse> rows = searchResult.getRows();
        List<QuerySubUserDepositListRet> data = new ArrayList<>();

        if (CollectionUtils.isNotEmpty(rows)) {
            for (ListDepositResponse row : rows) {
                data.add(CopyBeanUtils.fastCopy(row, QuerySubUserDepositListRet.class));
            }
        }

        return new CommonPageRet<>(data, searchResult.getTotal());
    }
}
