package com.binance.mgs.account.account.vo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.lang3.StringUtils;
import org.hibernate.validator.constraints.Length;

@Getter
@Setter
@ApiModel("用户注册确认")
public class RegisterConfirmV2Arg extends MultiCodeVerifyArg {

    private static final long serialVersionUID = -3275046861288689013L;
    @ApiModelProperty(value = "email")
    @Length(max = 255)
    private String email;
    @ApiModelProperty(value = "mobileCode")
    @Length(max = 10)
    private String mobileCode;
    @ApiModelProperty(value = "mobile")
    @Length(max = 50)
    private String mobile;

    public String getEmail() {
        return StringUtils.trimToEmpty(email).toLowerCase();
    }

}
