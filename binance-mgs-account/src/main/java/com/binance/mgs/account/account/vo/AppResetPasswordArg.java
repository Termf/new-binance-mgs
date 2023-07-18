package com.binance.mgs.account.account.vo;

import com.binance.platform.mgs.base.vo.CommonArg;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.apache.commons.lang3.StringUtils;
import org.hibernate.validator.constraints.NotBlank;
import org.hibernate.validator.constraints.NotEmpty;

@ApiModel("APP用户忘记密码邮箱验证")
@Data
@EqualsAndHashCode(callSuper = false)
public class AppResetPasswordArg extends CommonArg {
    /**
     *
     */
    private static final long serialVersionUID = 1761271881884724126L;

    @ApiModelProperty(value = "邮箱", required = true)
    @NotBlank
    private String email;

    @ApiModelProperty(value = "新密码", required = true)
    @NotEmpty
    private String password;

    @ApiModelProperty(value = "2次验证码", required = false)
    @NotEmpty
    private String verifyCode;

    public String getEmail() {
        return StringUtils.trimToEmpty(email).toLowerCase();
    }
}
