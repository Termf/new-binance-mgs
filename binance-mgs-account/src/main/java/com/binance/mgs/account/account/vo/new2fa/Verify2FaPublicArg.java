package com.binance.mgs.account.account.vo.new2fa;

import javax.validation.constraints.NotNull;

import com.binance.account2fa.enums.BizSceneEnum;
import com.binance.mgs.account.account.vo.MultiCodeVerifyArg;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import org.hibernate.validator.constraints.Length;

/**
 * Created by Kay.Zhao on 2022/3/1
 */
@Data
public class Verify2FaPublicArg extends MultiCodeVerifyArg {

    @ApiModelProperty("邮箱")
    @Length(max = 255)
    private String email;

    @ApiModelProperty(value = "mobileCode")
    @Length(max = 10)
    private String mobileCode;
    @ApiModelProperty(value = "mobile")
    @Length(max = 50)
    private String mobile;
    
    @ApiModelProperty("业务场景")
    @NotNull
    private String bizScene;

    @ApiModelProperty("用来续期登录态的token")
    private String refreshToken;

    @ApiModelProperty("requestId")
    private String requestId;

    @ApiModelProperty("安全验证sessionId")
    private String sessionId;
    
}
