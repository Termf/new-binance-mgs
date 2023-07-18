package com.binance.mgs.account.account.vo.new2fa;

import com.binance.platform.mgs.base.vo.CommonArg;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.validation.constraints.NotEmpty;
import java.util.HashMap;
import java.util.Map;

@ApiModel(description="发送邮件激活码")
@Data
@EqualsAndHashCode(callSuper = false)
public class SendNewEmailVerifyCodeArg extends CommonArg {
    private static final long serialVersionUID = 3811894527525306971L;

    @ApiModelProperty("邮箱")
    @NotEmpty
    private String email;

    @ApiModelProperty("是否是重新发送")
    private Boolean resend=false;

    @ApiModelProperty("业务场景")
    @NotEmpty
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
