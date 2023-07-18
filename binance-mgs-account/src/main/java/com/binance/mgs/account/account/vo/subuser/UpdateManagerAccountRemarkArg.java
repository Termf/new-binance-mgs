package com.binance.mgs.account.account.vo.subuser;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.NotBlank;
@Data
public class UpdateManagerAccountRemarkArg {

    @ApiModelProperty(required = true, notes = "托管账号email")
    @NotBlank
    private String managerSubUserEmail;

    @ApiModelProperty("备注")
    private String remark;
}
