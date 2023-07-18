package com.binance.mgs.account.account.vo.new2fa;

import java.util.HashMap;
import java.util.Map;

import com.binance.account2fa.enums.BizSceneEnum;
import com.binance.platform.mgs.base.vo.CommonArg;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

@ApiModel("发送邮件激活码")
@Data
@EqualsAndHashCode(callSuper = false)
public class SendEmailVerifyCodeArg extends CommonArg {
    private static final long serialVersionUID = 3811894527525306971L;

    @ApiModelProperty("是否是重新发送")
    private Boolean resend=false;

    @ApiModelProperty("业务场景")
    private String bizScene;

    @ApiModelProperty("流程Id-新邮箱验证场景需要")
    private String flowId;

    /**
     * 各场景所需邮件参数Key
     * api_key_manage:apiName
     */
    @ApiModelProperty("邮件所需参数")
    private Map<String, Object> params = new HashMap<>();
}
