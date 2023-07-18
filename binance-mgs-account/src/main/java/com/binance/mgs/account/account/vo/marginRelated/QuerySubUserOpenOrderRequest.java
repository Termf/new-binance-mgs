package com.binance.mgs.account.account.vo.marginRelated;

import com.binance.streamer.api.request.Pagination;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

/**
 * @author sean w
 * @date 2021/9/29
 **/
@Data
@ApiModel("查询子账户委托订单")
public class QuerySubUserOpenOrderRequest extends Pagination {

    private static final long serialVersionUID = -5742835030108703618L;

    @ApiModelProperty(required = true, name = "子账号邮箱")
    private String email;

    @ApiModelProperty(required = false, name = "产品代码")
    private String symbol;
}
