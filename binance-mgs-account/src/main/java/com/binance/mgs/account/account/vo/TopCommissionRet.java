package com.binance.mgs.account.account.vo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;

@Data
@ApiModel("佣金前三用户信息")
public class TopCommissionRet implements Serializable {

    private static final long serialVersionUID = -1152190185736273155L;
    @ApiModelProperty(value = "邮箱")
   private String  email;
    @ApiModelProperty(value = "佣金金额")
   private String  commission;

}