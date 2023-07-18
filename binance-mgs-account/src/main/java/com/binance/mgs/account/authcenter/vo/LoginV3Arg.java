package com.binance.mgs.account.authcenter.vo;

import org.apache.commons.lang3.StringUtils;
import org.hibernate.validator.constraints.Length;
import org.hibernate.validator.constraints.NotEmpty;

import com.binance.master.validator.groups.Select;
import com.binance.mgs.account.account.vo.MultiCodeVerifyArg;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.validation.constraints.NotBlank;
import java.io.Serializable;

@ApiModel("登录参数V3")
@Data
@EqualsAndHashCode(callSuper = false)
public class LoginV3Arg implements Serializable {

    private static final long serialVersionUID = 850308342818740970L;
    @ApiModelProperty(required = true, notes = "账号")
    @Length(max = 255)
    private String email;

    @ApiModelProperty(value = "mobileCode")
    @Length(max = 10)
    private String mobileCode;
    @ApiModelProperty(value = "mobile")
    @Length(max = 50)
    private String mobile;

    @ApiModelProperty(required = false, notes = "密码")
    @NotEmpty(groups = Select.class)
    private String password;
    @ApiModelProperty(required = true, notes = "新算法的密码")
    private String safePassword;
    @ApiModelProperty("fido验证码")
    private String fidoVerifyCode;


    @ApiModelProperty(value = "验证方式：gt 极验,reCAPTCHA 谷歌, 为空 阿里云", allowableValues = "gt,bCAPTCHA,reCAPTCHA")
    private String validateCodeType;
    @ApiModelProperty(value = "极验验证二次验证表单数据 chllenge")
    private String geetestChallenge;
    @ApiModelProperty(value = "极验验证二次验证表单数据 validate")
    private String geetestValidate;
    @ApiModelProperty(value = " 极验验证二次验证表单数据 seccode")
    private String geetestSecCode;
    @ApiModelProperty(value = "极验验证 服务器端缓存的id，验证时需要回传，web端不需要，会从cookie中直接获取")
    private String gtId;

    @ApiModelProperty(value = "谷歌验证")
    private String recaptchaResponse;

    @ApiModelProperty(value = "bCAPTCHA验证码token")
    private String bCaptchaToken;
    @ApiModelProperty(value = "安全验证sessionId")
    private String sessionId;

    @ApiModelProperty(required = false, notes = "是否走新的登录流程")
    private Boolean isNewLoginProcess=false;

    @ApiModelProperty(value = "登录flowId", required = true)
    @NotBlank
    private String loginFlowId;

    public void setEmail(String email) {
        this.email = StringUtils.trimToEmpty(email).toLowerCase();
    }

    public void setPassword(String password) {
        this.password = password == null ? null : password.trim();
    }
}
