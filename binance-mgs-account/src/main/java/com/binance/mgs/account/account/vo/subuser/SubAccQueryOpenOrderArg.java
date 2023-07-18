package com.binance.mgs.account.account.vo.subuser;

import com.binance.platform.mgs.base.vo.CommonPageArg;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

@ApiModel(description = "查询用户子账号未完成的订单列表 Request", value = "查询用户子账号未完成的订单列表 Request")
@Data
@EqualsAndHashCode(callSuper = false)
public class SubAccQueryOpenOrderArg extends CommonPageArg {
    private static final long serialVersionUID = -1190839780038209133L;
    /**
     *
     */

    @ApiModelProperty(required = false, notes = "产品代码")
    private String symbol;

    @ApiModelProperty(required = false, notes = "子账号邮箱，不传默认查询所有子账号信息")
    private String userId;

    /**
     * 因为一开始不支持分页，前端不会传值，故需要兼容下：若前端没传，则设置默认page=1及rows=1000
     */
    public SubAccQueryOpenOrderArg() {
        if (getPage() == null || getPage() < 1) {
            setPage(1);
        }
        if (getRows() == null || getRows() < 1000) {
            setRows(1000);
        }
    }

}
