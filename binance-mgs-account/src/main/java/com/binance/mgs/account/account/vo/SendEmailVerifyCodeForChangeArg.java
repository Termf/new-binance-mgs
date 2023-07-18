package com.binance.mgs.account.account.vo;

import com.binance.account.vo.security.enums.BizSceneEnum;
import com.binance.platform.mgs.base.vo.CommonArg;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.util.HashMap;
import java.util.Map;

@ApiModel("发送邮件激活码")
@Data
@EqualsAndHashCode(callSuper = false)
public class SendEmailVerifyCodeForChangeArg extends CommonArg {
    private static final long serialVersionUID = 3811894527125326971L;

    @ApiModelProperty("用户email")
    private String email;

    @ApiModelProperty("是否是重新发送")
    private Boolean resend=false;

    @ApiModelProperty("业务场景")
    @NotNull
    private BizSceneEnum bizScene;

    @ApiModelProperty("是否新邮箱")
    private Boolean isNewEmail = false;


    /**
     * 各场景所需邮件参数Key
     * api_key_manage:apiName
     */
    @ApiModelProperty("邮件所需参数")
    private Map<String, Object> params = new HashMap<>();
}
