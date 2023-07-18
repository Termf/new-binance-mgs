package com.binance.mgs.account.fido.vo;

import com.binance.platform.mgs.business.captcha.vo.ValidateCodeArg;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.hibernate.validator.constraints.Length;

import javax.validation.constraints.NotBlank;

@EqualsAndHashCode(callSuper = true)
@Data
public class AllCredentialPublicArg extends ValidateCodeArg {

    @ApiModelProperty("邮箱")
    @Length(max = 255)
    private String email;

    @ApiModelProperty(value = "mobileCode")
    @Length(max = 10)
    private String mobileCode;
    @ApiModelProperty(value = "mobile")
    @Length(max = 50)
    private String mobile;

    @ApiModelProperty("用来续期登录态的token")
    private String refreshToken;

    @ApiModelProperty("登录flowId")
    private String loginFlowId;
}
