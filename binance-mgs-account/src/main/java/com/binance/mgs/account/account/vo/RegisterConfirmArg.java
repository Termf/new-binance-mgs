package com.binance.mgs.account.account.vo;

import com.binance.platform.mgs.base.vo.CommonArg;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.validator.constraints.NotEmpty;

@Getter
@Setter
@ApiModel("用户注册邮箱确认")
public class RegisterConfirmArg extends CommonArg {
    private static final long serialVersionUID = 5342407486589484210L;
    @ApiModelProperty(required = true, notes = "用户编号")
    @NotEmpty
    private String userId;
    @ApiModelProperty(required = false, notes = "校验码")
    @NotEmpty
    private String verifyCode;

}
