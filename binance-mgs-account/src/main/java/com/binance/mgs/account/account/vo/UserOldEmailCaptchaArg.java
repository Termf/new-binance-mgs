package com.binance.mgs.account.account.vo;

import lombok.Data;
import org.hibernate.validator.constraints.NotBlank;


@Data
public class UserOldEmailCaptchaArg {

    @NotBlank
    private String flowId;

    @NotBlank
    private String emailVerifyCode; //邮箱验证码
}
