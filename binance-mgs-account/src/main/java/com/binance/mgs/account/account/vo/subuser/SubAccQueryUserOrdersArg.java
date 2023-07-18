package com.binance.mgs.account.account.vo.subuser;


import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

@ApiModel("子账号历史委托查询")
@Data
@EqualsAndHashCode(callSuper = false)
public class SubAccQueryUserOrdersArg extends QueryUserOrdersArg {

    private static final long serialVersionUID = -9061510856419884338L;
    /**
     *
     */

    @ApiModelProperty(required = false, notes = "子账号UserId，不传默认查询所有子账号信息")
    private String userId;
}
