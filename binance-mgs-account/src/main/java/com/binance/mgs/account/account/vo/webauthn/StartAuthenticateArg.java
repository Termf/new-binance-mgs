package com.binance.mgs.account.account.vo.webauthn;

import com.binance.platform.mgs.base.vo.CommonArg;
import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.validator.constraints.NotBlank;

@Getter
@Setter
public class StartAuthenticateArg extends CommonArg {

    @ApiModelProperty("Origin")
    @NotBlank
    private String origin;

    @ApiModelProperty("email")
    private String email;


    @ApiModelProperty(value = "mobileCode")
    private String mobileCode;
    @ApiModelProperty(value = "mobile")
    private String mobile;
}
