package com.binance.mgs.account.account.vo.subuser;

import com.binance.accountsubuser.vo.enums.FunctionAccountType;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

/**
 * @author sean w
 * @date 2022/4/19
 **/
@ApiModel("托管子账户各币种资产资产详情")
@Data
public class ManagerUserAvailableBalanceListArg {

    @NotBlank
    private String email;

    @ApiModelProperty("FROM账户类型")
    @NotNull
    private FunctionAccountType type;

    @ApiModelProperty(required = false, value = "交易对")
    private String symbol;
}
