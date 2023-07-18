package com.binance.mgs.account.account.vo.subuser;

import com.binance.accountsubuser.vo.enums.FunctionAccountType;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.NotNull;

@ApiModel("托管子账户划转历史")
@Data
public class ManagerSubUserTransferLogArg {

    @NotNull
    private Integer page;

    @NotNull
    private Integer rows;

    @ApiModelProperty("查询指定托管子账户的划转历史")
    private String managerSubUserEmail;

    @ApiModelProperty("划转方(to:划入方;from:划出方;默认查所有)")
    private String transfers;

    @ApiModelProperty("查询划转起始时间")
    private Long startTime;

    @ApiModelProperty("查询划转结束时间")
    private Long endTime;

    @ApiModelProperty(required = true, notes = "转出方账户类型，比方说从现货转出那么就填spot")
    private FunctionAccountType senderFunctionAccountType;
}
