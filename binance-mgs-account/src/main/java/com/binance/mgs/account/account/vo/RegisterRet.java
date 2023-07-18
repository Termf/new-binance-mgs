package com.binance.mgs.account.account.vo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;

@Data
@ApiModel("注册")
public class RegisterRet implements Serializable {
    /**
     * 
     */
    private static final long serialVersionUID = -5415160785881220425L;
    @ApiModelProperty(value = "用户ID")
    private Long userId;
}
