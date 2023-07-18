package com.binance.mgs.account.account.enums;

import com.binance.assetservice.enums.TransferType;
import com.binance.margin.isolated.api.transfer.enums.TransferTarget;
import io.swagger.annotations.ApiModel;
import lombok.Getter;

import static com.binance.margin.isolated.api.transfer.enums.TransferTarget.ISOLATED_MARGIN;
import static com.binance.margin.isolated.api.transfer.enums.TransferTarget.SPOT;

@ApiModel("划转类型")
public enum SubUserTransferKindType {

    SPOT_ISOLATED_MARGIN(1, "现货账户与逐仓之间划转", TransferType.ISOLATED_MARGIN_TRANSFER, SPOT, ISOLATED_MARGIN),
    ISOLATED_MARGIN_SPOT(2, "现货账户与逐仓之间划转", TransferType.ISOLATED_MARGIN_TRANSFER, ISOLATED_MARGIN, SPOT);

    SubUserTransferKindType(Integer code, String desc, TransferType type, TransferTarget from, TransferTarget to) {
        this.code = code;
        this.desc = desc;
        this.type = type.getCode();
        this.from = from;
        this.to = to;
    }

    private Integer code;

    private String desc;

    private Integer type;

    @Getter
    private TransferTarget from;

    @Getter
    private TransferTarget to;

    public static boolean isFromIsolatedMargin(SubUserTransferKindType kindType) {
        return kindType.from == ISOLATED_MARGIN;
    }

    public static boolean isToIsolatedMarin(SubUserTransferKindType kindType) {
        return kindType.to == ISOLATED_MARGIN;
    }
}
