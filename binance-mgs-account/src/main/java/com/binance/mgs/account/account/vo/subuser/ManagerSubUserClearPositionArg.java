package com.binance.mgs.account.account.vo.subuser;

import com.binance.accountsubuser.vo.enums.FunctionAccountType;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.NotBlank;

/**
 * @author sean w
 * @date 2022/3/11
 **/
@Data
@ApiModel("母账户一键平仓托管子账户Request")
public class ManagerSubUserClearPositionArg {

    @ApiModelProperty("托管子账户邮箱")
    @NotBlank
    private String managerSubUserEmail;

    @ApiModelProperty("需要平仓的交易对，不传默认为平所有")
    private String symbol = "ALL";

    @ApiModelProperty("账户类型，只允许Future/Delivery")
    private FunctionAccountType accountType;
}
