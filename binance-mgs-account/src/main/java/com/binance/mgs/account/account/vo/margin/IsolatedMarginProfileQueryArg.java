package com.binance.mgs.account.account.vo.margin;

import com.binance.margin.isolated.api.profit.request.PeriodType;
import com.binance.platform.mgs.base.vo.CommonArg;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;

@Data
@ApiModel
public class IsolatedMarginProfileQueryArg extends CommonArg {

    private static final long serialVersionUID = 6688823820488851157L;

    @ApiModelProperty(required = true, name = "子账号邮箱")
    @NotEmpty
    private String email;

    @ApiModelProperty(required = true, name = "PeriodType")
    @NotNull
    private PeriodType type;
}
