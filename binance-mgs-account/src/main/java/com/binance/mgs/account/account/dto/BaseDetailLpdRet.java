package com.binance.mgs.account.account.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;

@Data
@ApiModel("获取用户详细信息LPD")
public class BaseDetailLpdRet implements Serializable {

    private static final long serialVersionUID = 2375900052673256444L;
    @ApiModelProperty(value = "邮箱")
    private String email;
}
