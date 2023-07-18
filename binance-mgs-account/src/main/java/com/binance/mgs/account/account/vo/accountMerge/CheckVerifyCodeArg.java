package com.binance.mgs.account.account.vo.accountMerge;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import org.apache.commons.lang3.StringUtils;
import org.hibernate.validator.constraints.Length;

@ApiModel("CheckVerifyCodeArg")
@Data
public class CheckVerifyCodeArg {

    @ApiModelProperty("被合并账号的邮箱")
    @Length(max = 255)
    private String mergedEmail;

    @ApiModelProperty("被合并账号的手机mobileCode")
    @Length(max = 10)
    private String mergedMobileCode;

    @ApiModelProperty("被合并账号的手机号")
    @Length(max = 50)
    private String mergedMobile;

    @ApiModelProperty("被合并账号的手机验证码")
    private String mergedMobileVerifyCode;

    @ApiModelProperty("被合并账号的邮件验证码")
    private String mergedEmailVerifyCode;
    
    @ApiModelProperty("手机验证码")
    private String mobileVerifyCode;
    
    @ApiModelProperty("邮件验证码")
    private String emailVerifyCode;

    public String getMergedEmail() {
        return StringUtils.trimToEmpty(mergedEmail).toLowerCase();
    }
    
}
