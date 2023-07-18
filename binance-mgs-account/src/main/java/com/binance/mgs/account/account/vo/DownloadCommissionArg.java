package com.binance.mgs.account.account.vo;

import com.binance.platform.mgs.base.vo.CommonArg;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.validation.constraints.NotNull;

@ApiModel("佣金Excel导出")
@Data
@EqualsAndHashCode(callSuper = false)
public class DownloadCommissionArg extends CommonArg {

    private static final long serialVersionUID = 8705195798220507040L;
    @ApiModelProperty(required = false, notes = "开始时间")
    @NotNull
    private Long startTime;

    @ApiModelProperty(required = false, notes = "结束时间")
    @NotNull
    private Long endTime;

    @ApiModelProperty(required = false, notes = "返佣类型，0为交易返佣，1为法币返佣")
    private int commissionType = 0;
}
