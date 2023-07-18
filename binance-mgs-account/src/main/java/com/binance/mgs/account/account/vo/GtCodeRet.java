package com.binance.mgs.account.account.vo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;

@Data
@ApiModel("极验返回值")
public class GtCodeRet implements Serializable {

    /**
     * 
     */
    private static final long serialVersionUID = 4568233125303218059L;

    @ApiModelProperty(value = "challenge")
    private String challenge;

    @ApiModelProperty(value = "gt")
    private String gt;
    @ApiModelProperty(value = "gt服务器端缓存的id，验证时需要回传，web端不需要，会从cookie中直接获取")
    private String gtId;

}
