package com.binance.mgs.account.account.vo.subuser;

import com.binance.platform.mgs.base.vo.CommonPageArg;
import com.binance.platform.mgs.enums.AccountType;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

@ApiModel("历史委托查询")
@Data
@EqualsAndHashCode(callSuper = false)
public class QueryUserOrdersArg extends CommonPageArg {

    private static final long serialVersionUID = 1317171964067484113L;
    /**
     *
     */

    @ApiModelProperty(required = false, notes = "开始时间")
    private Long startTime;

    @ApiModelProperty(required = false, notes = "结束时间")
    private Long endTime;

    @ApiModelProperty(required = false, notes = "产品代码")
    private String symbol;

    @ApiModelProperty(required = false, notes = "买卖方向")
    private String direction;

    @ApiModelProperty(required = false, notes = "状态(NEW: 未成交, Partial Fill: 部分成交, Filled: 全部成交, Canceled: 已撤销)")
    private String status;

    @ApiModelProperty(required = false, notes = "是否隐藏 已撤销")
    private Boolean hideCancel;

    @ApiModelProperty(required = false, notes = "基础资产")
    private String baseAsset;

    @ApiModelProperty(required = false, notes = "标价货币")
    private String quoteAsset;
    @ApiModelProperty(required = false, notes = "账户类型，默认为MAIN主账户")
    private AccountType accountType = AccountType.MAIN;
}
