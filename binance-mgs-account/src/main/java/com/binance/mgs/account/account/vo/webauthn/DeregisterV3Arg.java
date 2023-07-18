package com.binance.mgs.account.account.vo.webauthn;

import com.alibaba.fastjson.JSONObject;
import com.binance.mgs.account.account.vo.MultiCodeVerifyArg;
import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;

import javax.validation.constraints.NotNull;

@Getter
@Setter
public class DeregisterV3Arg extends MultiCodeVerifyArg {
    private static final long serialVersionUID = 7484595671566809705L;

    @ApiModelProperty("验证详情信息, 需要符合JSON格式, 能序列化为RegistrationFinishRequest对象,内容主要有：requestId(开始请求中返回的requestId), credential(公钥签名验证信息)")
    @NotNull
    private JSONObject finishDetail;

    @ApiModelProperty("yubikey credentialId")
    private String credentialId;

}
