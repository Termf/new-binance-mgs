package com.binance.mgs.account.account.vo.new2fa;

import com.binance.platform.mgs.base.vo.CommonArg;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

/**
 * Created by Kay.Zhao on 2022/11/25
 */
@Data
public class QueryRoamingStatusArg extends CommonArg {

    @ApiModelProperty("roaming验证流水号")
    private String roamingFlowId;
    
}
