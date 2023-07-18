package com.binance.mgs.account.account.vo.subuser;

import com.binance.accountsubuser.vo.enums.FunctionAccountType;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.NotBlank;

@Data
@ApiModel("母账户一键平仓子账户Request")
public class ClearPositionArg {

    @ApiModelProperty("子账户邮箱")
    @NotBlank
    private String subUserEmail;

    @ApiModelProperty("需要平仓的交易对，不传默认为平所有")
    private String symbol = "ALL";

    @ApiModelProperty("账户类型，只允许Future/Delivery")
    private FunctionAccountType accountType;
}
