package com.binance.mgs.account.account.vo;

import com.binance.platform.mgs.business.captcha.vo.ValidateCodeArg;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.apache.commons.lang3.StringUtils;
import org.hibernate.validator.constraints.NotBlank;

@ApiModel("APP忘记密码-发送重置验证码邮件")
@Data
@EqualsAndHashCode(callSuper = false)
public class AppForgetPasswordArg extends ValidateCodeArg {

    /**
     * 
     */
    private static final long serialVersionUID = -7754201576845354092L;

    @ApiModelProperty(value = "邮箱", required = true)
    @NotBlank
    private String email;

    @ApiModelProperty(value = "验证方式：gt 极验,reCAPTCHA 谷歌, 为空 阿里云")
    private String validateCodeType;

    public String getEmail() {
        return StringUtils.trimToEmpty(email).toLowerCase();
    }

}
