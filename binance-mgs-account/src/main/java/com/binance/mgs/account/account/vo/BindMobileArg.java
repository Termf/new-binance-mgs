package com.binance.mgs.account.account.vo;

import com.binance.platform.mgs.base.vo.CommonArg;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.validator.constraints.NotBlank;

@ApiModel("绑定手机Arg")
@Getter
@Setter
public class BindMobileArg extends CommonArg {

    private static final long serialVersionUID = 6910076833119975017L;

    @ApiModelProperty("手机号")
    @NotBlank
    private String mobile;

    @ApiModelProperty("手机代码")
    @NotBlank
    private String mobileCode;

    @ApiModelProperty("手机验证码")
    @NotBlank
    private String smsCode;

    @ApiModelProperty("谷歌验证码")
    private Integer googleCode;
}
