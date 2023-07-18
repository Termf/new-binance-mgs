package com.binance.mgs.account.account.vo;

import com.binance.platform.mgs.base.vo.CommonArg;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@ApiModel("设置用户默认配置项")
public class GetUserConfigArg extends CommonArg {


    /**
     * 
     */
    private static final long serialVersionUID = -8037851600537075793L;

    @ApiModelProperty(value = "配置项类型名", required = false)
    private String configType;

    @ApiModelProperty(value = "排除项", required = false)
    private String exclude;

    @ApiModelProperty(value = "针对没有的配置，是否进行本土化推荐", required = false)
    private Boolean needLocalRecommend;

}
