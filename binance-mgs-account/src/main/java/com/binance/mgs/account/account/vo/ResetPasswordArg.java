package com.binance.mgs.account.account.vo;

import com.binance.platform.mgs.business.captcha.vo.ValidateCodeArg;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.hibernate.validator.constraints.Length;
import org.hibernate.validator.constraints.NotEmpty;

@ApiModel("重置密码")
@Data
@EqualsAndHashCode(callSuper = false)
public class ResetPasswordArg extends ValidateCodeArg {
    private static final long serialVersionUID = -7548451034936837245L;
    @ApiModelProperty(required = true, notes = "邮箱")
    @NotEmpty
    @Length(max = 255)
    private String email;
    @ApiModelProperty(required = true, notes = "授权码")
    @NotEmpty
    private String verifyCode;
    @ApiModelProperty(required = true, notes = "新的密码")
    @NotEmpty
    private String password;

    @ApiModelProperty(required = false, notes = "操作来源")
    private String operateSource;


}
