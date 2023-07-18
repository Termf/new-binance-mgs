package com.binance.mgs.account.account.vo.subuser;

import com.binance.mgs.account.account.vo.MultiCodeVerifyArg;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.NotNull;

@Data
public class BindManagerAccountArg extends MultiCodeVerifyArg {

    @ApiModelProperty(required = true, notes = "母账号UserId")
    @NotNull
    private Long tradeParentUserId;

    @ApiModelProperty(required = true, notes = "母账号email")
    @NotNull
    private String tradeParentEmail;

    @ApiModelProperty(required = false, notes = "主账号")
    @NotNull
    private Long rootUserId;

    @ApiModelProperty(required = false, notes = "托管账号email")
    @NotNull
    private String managerSubUserEmail;


}
