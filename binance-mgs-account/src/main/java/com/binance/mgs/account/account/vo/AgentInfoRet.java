package com.binance.mgs.account.account.vo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;

@Data
@ApiModel("获取代理人信息")
public class AgentInfoRet implements Serializable {

    /**
     * 
     */
    private static final long serialVersionUID = 8598738886801452137L;
    @ApiModelProperty(value = "是否存在")
    private boolean isExist;
    @ApiModelProperty(value = "是否已经认证过")
    private boolean isVerified;

}
