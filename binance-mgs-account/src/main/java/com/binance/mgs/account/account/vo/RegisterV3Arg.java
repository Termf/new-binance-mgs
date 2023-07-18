package com.binance.mgs.account.account.vo;

import com.binance.account.vo.user.enums.RegisterationMethodEnum;
import com.binance.master.validator.groups.Add;
import com.binance.platform.mgs.business.captcha.vo.ValidateCodeArg;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.lang3.StringUtils;
import org.hibernate.validator.constraints.Length;
import org.hibernate.validator.constraints.NotEmpty;

import javax.validation.constraints.NotNull;

@Getter
@Setter
@ApiModel("RegisterV3Arg")
public class RegisterV3Arg extends ValidateCodeArg {

    @ApiModelProperty("手机验证码")
    private String mobileVerifyCode;
    @ApiModelProperty("google验证码")
    private String googleVerifyCode;
    @ApiModelProperty("邮件验证码")
    private String emailVerifyCode;
    @ApiModelProperty("yubikey验证码")
    private String yubikeyVerifyCode;

    /**
     * 
     */
    private static final long serialVersionUID = 3941484932746658174L;

    @ApiModelProperty(required = true, notes = "邮箱")
    @Length(max = 255)
    private String email;


    @ApiModelProperty(value = "mobileCode")
    @Length(max = 10)
    private String mobileCode;
    @ApiModelProperty(value = "mobile")
    @Length(max = 50)
    private String mobile;


    @ApiModelProperty(required = true, notes = "密码")
    @NotEmpty(groups = Add.class)
    private String password;
    @ApiModelProperty(required = true, notes = "新算法的密码")
    @NotNull
    private String safePassword;

    @ApiModelProperty(required = false, notes = "推荐人")
    private String agentId;
    @ApiModelProperty(required = false, notes = "注册渠道，对应原先的ts字段")
    private String registerChannel;
    @ApiModelProperty(required = false, notes = "是否走新的注册流程")

    private Boolean isNewRegistrationProcess=true;


    @ApiModelProperty(required = false, notes = "是否是期货一键开户流程")
    private Boolean isFastCreatFuturesAccountProcess=false;

    @ApiModelProperty("期货返佣推荐码")
    private String futuresReferalCode;

    @ApiModelProperty("注册方式(默认邮箱)")
    private RegisterationMethodEnum registerationMethod= RegisterationMethodEnum.EMAIL;


    @ApiModelProperty(required = false, notes = "是否检查ip")
    private Boolean checkIp=true;

    @ApiModelProperty(required = false, notes = "是否订阅邮件运营通知")
    private Boolean isEmailPromote;

    @ApiModelProperty(required = false, notes = "用户居住国家（前台勾选）")
    private String residentCountry;

    @ApiModelProperty(required = false,name = "utm source")
    private String source;

    @ApiModelProperty("个人或者企业账户,默认个人账户")
    private Boolean isPersonalAccount = true;

    @ApiModelProperty("是否统计agentCodeError")
    private Boolean isStatAgentError = false;

    @ApiModelProperty("oauth client id")
    private String oauthClientId;

    /**
     * 验证token，sms/email、verifytoken 二选一
     */
    @ApiModelProperty("验证token")
    private String verifyToken;

    public String getEmail() {
        return StringUtils.trimToEmpty(email).toLowerCase();
    }
}
