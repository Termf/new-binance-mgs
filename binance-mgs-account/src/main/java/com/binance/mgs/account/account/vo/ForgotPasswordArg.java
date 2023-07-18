package com.binance.mgs.account.account.vo;

import com.binance.platform.mgs.business.captcha.vo.ValidateCodeArg;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.hibernate.validator.constraints.NotEmpty;

@ApiModel("忘记密码")
@Data
@EqualsAndHashCode(callSuper = false)
public class ForgotPasswordArg extends ValidateCodeArg {
    private static final long serialVersionUID = -7548451034936837245L;
    @ApiModelProperty(required = true, notes = "邮箱")
    @NotEmpty
    private String email;
}
