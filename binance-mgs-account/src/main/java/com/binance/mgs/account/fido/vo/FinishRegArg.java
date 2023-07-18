package com.binance.mgs.account.fido.vo;

import javax.validation.constraints.NotNull;

import com.binance.account2fa.enums.BizSceneEnum;
import com.binance.platform.mgs.base.vo.CommonArg;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;


@EqualsAndHashCode(callSuper = true)
@Data
public class FinishRegArg extends CommonArg {

    @ApiModelProperty("One-time binding ID, used to return when binding is completed")
    @NotNull
    private String requestId;
    @ApiModelProperty("Front-end generated assertion information(The result of authenticator)." +
            "Need do a conversion from byte array to string for rawId, response.clientDataJSON, response.attestationObject. ")
    @NotNull
    private String createOpt;

    private String verifyToken;

    @ApiModelProperty("业务场景")
    private BizSceneEnum bizScene;

}
