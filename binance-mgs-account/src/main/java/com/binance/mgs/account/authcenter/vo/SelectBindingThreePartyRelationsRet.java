package com.binance.mgs.account.authcenter.vo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;

@Data
@ApiModel("三方绑定信息返回返回")
public class SelectBindingThreePartyRelationsRet {

    private String thirdEmail;
    private String registerChannel;
}