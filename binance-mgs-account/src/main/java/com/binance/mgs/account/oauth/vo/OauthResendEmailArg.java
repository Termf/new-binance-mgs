package com.binance.mgs.account.oauth.vo;

import com.binance.platform.mgs.business.captcha.vo.ValidateCodeArg;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import org.hibernate.validator.constraints.NotBlank;

@ApiModel(description = "oauth重发激活邮件Request")
@Data
public class OauthResendEmailArg extends ValidateCodeArg {
    private static final long serialVersionUID = 1155868511297665105L;
    @ApiModelProperty(required = true, notes = "邮箱")
    @NotBlank
    private String email;

    public void setEmail(String email) {
        this.email = email == null ? null : email.toLowerCase().trim();
    }
}
