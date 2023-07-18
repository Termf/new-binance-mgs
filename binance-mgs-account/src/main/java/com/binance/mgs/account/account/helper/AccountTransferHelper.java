package com.binance.mgs.account.account.helper;

import com.binance.margin.isolated.api.transfer.TransferApi;
import com.binance.margin.isolated.api.transfer.request.TransferRequest;
import com.binance.master.error.BusinessException;
import com.binance.master.error.GeneralCode;
import com.binance.master.models.APIRequest;
import com.binance.master.models.APIResponse;
import com.binance.mgs.account.account.enums.SubUserTransferKindType;
import com.binance.mgs.account.account.vo.subuser.SubUserTransferIsolatedMarginArg;
import com.binance.platform.mgs.base.helper.BaseHelper;
import io.swagger.annotations.ApiModel;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@ApiModel("账户划转处理")
@Component
public class AccountTransferHelper extends BaseHelper {

    @Autowired
    private TransferApi transferApi;

    public void TransferOutFromIsolatedMargin(SubUserTransferIsolatedMarginArg arg, Long subUserId) {
        TransferRequest transferRequest = buildIsolatedMarginTransferRequest(arg, subUserId);
        APIResponse<Long> response = transferApi.transferOut(APIRequest.instance(transferRequest));
        checkResponse(response);
    }

    public void transferInToIsolatedMargin(SubUserTransferIsolatedMarginArg arg, Long subUserId) {
        TransferRequest transferRequest = buildIsolatedMarginTransferRequest(arg, subUserId);
        APIResponse<Long> response = transferApi.transferIn(APIRequest.instance(transferRequest));
        checkResponse(response);
    }

    private TransferRequest buildIsolatedMarginTransferRequest(SubUserTransferIsolatedMarginArg transferArg, Long subUserId) {
        TransferRequest request = new TransferRequest();
        BeanUtils.copyProperties(transferArg, request);
        request.setUserId(subUserId);
        if (SubUserTransferKindType.isFromIsolatedMargin(transferArg.getKindType())) {
            request.setTarget(transferArg.getKindType().getTo());
            return request;
        } else if (SubUserTransferKindType.isToIsolatedMarin(transferArg.getKindType())) {
            request.setTarget(transferArg.getKindType().getFrom());
            return request;
        }
        throw new BusinessException(GeneralCode.ILLEGAL_PARAM.getCode(), "kindType must by SPOT_ISOLATED_MARGIN or ISOLATED_MARGIN_SPOT");
    }
}
