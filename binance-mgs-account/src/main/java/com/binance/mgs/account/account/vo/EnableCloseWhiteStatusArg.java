package com.binance.mgs.account.account.vo;

import com.binance.platform.mgs.base.vo.CommonArg;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

@ApiModel("关闭提现白名单的邮箱确认")
@Data
@EqualsAndHashCode(callSuper = false)
public class EnableCloseWhiteStatusArg extends CommonArg {


    /**
     * 
     */
    private static final long serialVersionUID = -5946507008970040238L;
    @ApiModelProperty(required = true, notes = "jwt token")
    private String jwtToken;

}
