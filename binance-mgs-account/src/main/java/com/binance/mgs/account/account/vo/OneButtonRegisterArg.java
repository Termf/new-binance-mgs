package com.binance.mgs.account.account.vo;

import com.binance.platform.mgs.business.captcha.vo.ValidateCodeArg;
import org.apache.commons.lang3.StringUtils;

import com.binance.account.vo.user.enums.RegisterationMethodEnum;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.validator.constraints.Length;

@Getter
@Setter
@ApiModel("OneButtonRegisterArg")
public class OneButtonRegisterArg extends ValidateCodeArg {


    private static final long serialVersionUID = -1558470281468492586L;
    @ApiModelProperty(required = true, notes = "邮箱")
    @Length(max = 255)
    private String email;

    @ApiModelProperty(required = false, notes = "渠道")
    private String trackSource;

    @ApiModelProperty("手机号")
    @Length(max = 50)
    private String mobile;

    @ApiModelProperty("手机代码")
    @Length(max = 10)
    private String mobileCode;

    @ApiModelProperty("注册方式(默认邮箱)")
    private RegisterationMethodEnum registerationMethod=RegisterationMethodEnum.EMAIL;

    @ApiModelProperty(required = false, notes = "推荐人")
    private Long agentId;

    public String getEmail() {
        return StringUtils.trimToEmpty(email).toLowerCase();
    }
}
