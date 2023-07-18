package com.binance.mgs.account.account.vo;

import com.binance.platform.mgs.base.vo.CommonArg;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

@ApiModel("推荐人信息查询")
@Data
@EqualsAndHashCode(callSuper = false)
public class AgentInfoArg extends CommonArg {

    /**
     * 
     */
    private static final long serialVersionUID = 6163766904390710213L;
    @ApiModelProperty(value = "推荐人id", required = true)
    private String agentId;
}
