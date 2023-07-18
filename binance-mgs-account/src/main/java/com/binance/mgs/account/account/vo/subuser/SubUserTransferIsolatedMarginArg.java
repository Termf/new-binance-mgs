package com.binance.mgs.account.account.vo.subuser;

import com.binance.mgs.account.account.enums.SubUserTransferKindType;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.math.BigDecimal;

@ApiModel("子账户isolated-margin划转实体")
@Data
public class SubUserTransferIsolatedMarginArg {

    @ApiModelProperty("子账户邮箱")
    @NotEmpty(message = "subUserEmail is not null")
    private String subUserEmail;

    @ApiModelProperty("划转数量")
    @NotNull
    private BigDecimal amount;

    @ApiModelProperty("划转币种")
    @NotEmpty
    private String asset;

    @ApiModelProperty("币对")
    @NotEmpty
    private String symbol;

    @ApiModelProperty("划转类型")
    @NotNull
    private SubUserTransferKindType kindType;
}
