package com.binance.mgs.account.account.vo.delivery;

import com.binance.master.commons.ToString;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;

import javax.validation.constraints.NotBlank;

@ApiModel(description = "查询子账户open order Request", value = "查询open order Request")
@Getter
@Setter
public class QuerySubUserOpenOrderRequest extends ToString {

    private static final long serialVersionUID = -367928525540616853L;


    @ApiModelProperty(required = true, value = "子账户邮箱")
    private String email;

    @ApiModelProperty(required = false, notes = "产品代码")
    private String symbol;

    @ApiModelProperty(required = false, notes = "基础资产")
    private String baseAsset;

    @ApiModelProperty(required = false, notes = "标价货币")
    private String quoteAsset;
}
