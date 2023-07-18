package com.binance.mgs.account.account.vo;

import com.binance.platform.mgs.business.captcha.vo.ValidateCodeArg;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.hibernate.validator.constraints.Length;
import org.hibernate.validator.constraints.NotEmpty;

@ApiModel("ResetPasswordArgV2")
@Data
@EqualsAndHashCode(callSuper = false)
public class ResetPasswordArgV2  extends ValidateCodeArg {
    @ApiModelProperty(value = "email")
    @Length(max = 255)
    private String email;
    @ApiModelProperty(value = "mobileCode")
    @Length(max = 10)
    private String mobileCode;
    @ApiModelProperty(value = "mobile")
    @Length(max = 50)
    private String mobile;
    @ApiModelProperty(required = true, notes = "token")
    @NotEmpty
    private String token;
    @ApiModelProperty(required = true, notes = "新的密码")
    @NotEmpty
    private String password;
    @ApiModelProperty(required = true, notes = "新算法密码")
    @NotEmpty
    private String safePassword;



}
