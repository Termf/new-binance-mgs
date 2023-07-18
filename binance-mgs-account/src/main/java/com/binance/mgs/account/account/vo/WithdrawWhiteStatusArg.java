package com.binance.mgs.account.account.vo;

import com.binance.master.enums.AuthTypeEnum;
import com.binance.platform.mgs.base.vo.CommonArg;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

@ApiModel("开启或关闭提现白名单")
@Data
@EqualsAndHashCode(callSuper = false)
public class WithdrawWhiteStatusArg extends CommonArg {

    /**
     * 
     */
    private static final long serialVersionUID = 2686613633743163734L;

    @ApiModelProperty(required = false, notes = "认证类型")
    private AuthTypeEnum authType;

    @ApiModelProperty(required = false, notes = "2次验证码")
    private String verifyCode;

}
