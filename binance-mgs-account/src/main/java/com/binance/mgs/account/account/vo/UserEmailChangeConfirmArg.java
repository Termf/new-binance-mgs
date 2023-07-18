package com.binance.mgs.account.account.vo;

import com.binance.platform.mgs.base.vo.CommonArg;
import io.swagger.annotations.ApiModel;
import lombok.Data;
import org.hibernate.validator.constraints.NotBlank;


@Data
@ApiModel("用户确认新邮箱")
public class UserEmailChangeConfirmArg extends CommonArg {

    @NotBlank
    private String flowId;

    @NotBlank
    private String email;

    @NotBlank
    private String pwd;

    private String newSafePwd;

    @NotBlank
    private String code;

    @NotBlank
    private String authType;//sms ,google,fido2
}
