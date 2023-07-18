package com.binance.mgs.account.fido.vo;

import com.binance.account2fa.enums.BizSceneEnum;
import com.binance.platform.mgs.base.vo.CommonArg;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.validation.constraints.NotBlank;

@EqualsAndHashCode(callSuper = true)
@Data
public class DeleteCredentialArg extends CommonArg{
    
    @ApiModelProperty("credential id")
    @NotBlank
    private String credentialId;
    
    private String verifyToken;

    @ApiModelProperty("业务场景")
    private BizSceneEnum bizScene;
}
