package com.binance.mgs.account.account.vo.subuser;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.NotNull;

/**
 * @author sean w
 * @date 2022/2/28
 **/
@Data
public class DisableManagerSubUserArg {

    @ApiModelProperty(required = true, notes = "托管账号email")
    @NotNull
    private String managerSubUserEmail;
}
