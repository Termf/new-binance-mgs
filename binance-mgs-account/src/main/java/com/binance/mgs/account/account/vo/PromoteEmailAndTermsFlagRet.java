package com.binance.mgs.account.account.vo;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;


@Data
@ApiModel("获取用户是否展示营销邮件和服务条款勾选框")
public class PromoteEmailAndTermsFlagRet {

    @ApiModelProperty(value = "是否展示同意接收营销邮件勾选框")
    private Boolean promoteEmail;

    @ApiModelProperty(value = "是否展示Terms&privacy勾选框")
    private Boolean termsAndPrivacy;
}
