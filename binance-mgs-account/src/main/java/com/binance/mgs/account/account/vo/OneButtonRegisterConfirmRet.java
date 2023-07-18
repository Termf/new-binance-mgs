package com.binance.mgs.account.account.vo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;

@Data
@ApiModel("一键注册用户激活Response")
public class OneButtonRegisterConfirmRet implements Serializable {

    @ApiModelProperty(value = "uuid")
    private String uuid;


}
