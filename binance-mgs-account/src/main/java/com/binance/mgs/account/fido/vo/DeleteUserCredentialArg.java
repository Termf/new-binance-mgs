package com.binance.mgs.account.fido.vo;

import javax.validation.constraints.NotBlank;

import com.binance.platform.mgs.base.vo.CommonArg;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
public class DeleteUserCredentialArg extends CommonArg{
    
    @ApiModelProperty("clientId")
    @NotBlank
    private String clientId;
    
    private String verifyToken;
}
