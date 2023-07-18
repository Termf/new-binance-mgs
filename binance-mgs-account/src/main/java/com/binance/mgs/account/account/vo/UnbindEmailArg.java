package com.binance.mgs.account.account.vo;

import com.binance.platform.mgs.base.vo.CommonArg;
import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.validator.constraints.NotEmpty;

@Getter
@Setter
public class UnbindEmailArg extends CommonArg {

    @ApiModelProperty("密码")
    @NotEmpty
    private String password;
    @ApiModelProperty("邮件验证码")
    @NotEmpty
    private String emailCode;

}
