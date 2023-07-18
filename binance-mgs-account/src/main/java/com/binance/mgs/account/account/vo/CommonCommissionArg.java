package com.binance.mgs.account.account.vo;

import com.binance.platform.mgs.base.vo.CommonPageArg;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * @author zhenleisun
 */
@ApiModel("返佣相关的通用Request")
@Data
@EqualsAndHashCode(callSuper = false)
public class CommonCommissionArg extends CommonPageArg {
    private static final long serialVersionUID = -7129691248060707424L;

    @ApiModelProperty(required = false, notes = "返佣类型，0为交易返佣，1为法币返佣")
    private int commissionType = 0;
}
