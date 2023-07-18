package com.binance.mgs.account.account.vo;

import lombok.Data;
import org.hibernate.validator.constraints.NotBlank;

@Data
public class NewEmailCaptchaArg {
    @NotBlank
    private String flowId;

    @NotBlank
    private String emailVerifyCode; //邮箱验证码

    private String smsVerifyCode;// 手机验证码

    private String googleVerifyCode;//google 验证码
}
