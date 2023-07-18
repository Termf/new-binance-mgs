package com.binance.mgs.account.account.vo.webauthn;

import com.alibaba.fastjson.JSONObject;
import com.binance.master.enums.AuthTypeEnum;
import com.binance.platform.mgs.base.vo.CommonArg;
import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;

import javax.validation.constraints.NotNull;


@Getter
@Setter
public class DeregisterArg extends CommonArg {


    @ApiModelProperty("验证详情信息, 需要符合JSON格式, 能序列化为：AssertionFinishRequest对象," +
            " 内容主要有：requestId(开始请求中返回的requestId), credential(公钥签名验证信息)")
    @NotNull
    private JSONObject finishDetail;

    @ApiModelProperty("验证类型")
    private AuthTypeEnum authType;

    @ApiModelProperty("验证码")
    private String code;
}
