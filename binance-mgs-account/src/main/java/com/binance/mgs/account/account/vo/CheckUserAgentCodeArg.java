package com.binance.mgs.account.account.vo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import org.hibernate.validator.constraints.NotEmpty;

import java.io.Serializable;

/**
 * Created by yangyang on 2019/7/11.
 */
@Data
@ApiModel("check推荐人生成的code")
public class CheckUserAgentCodeArg implements Serializable{


    @ApiModelProperty(required = true, notes = "推荐人生成的code")
    @NotEmpty
    private String agentCode;
}
