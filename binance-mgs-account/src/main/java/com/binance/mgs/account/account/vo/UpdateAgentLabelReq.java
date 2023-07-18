package com.binance.mgs.account.account.vo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import org.hibernate.validator.constraints.NotBlank;

import java.io.Serializable;

/**
 * Created by yangyang on 2019/7/12.
 */
@Data
@ApiModel("更新agentLink")
public class UpdateAgentLabelReq implements Serializable {

    @ApiModelProperty(required = true, notes = "推荐人agentCode")
    @NotBlank
    private String agentCode;

    @ApiModelProperty(required = false, notes = "label标记")
    @NotBlank
    private String label;

}
