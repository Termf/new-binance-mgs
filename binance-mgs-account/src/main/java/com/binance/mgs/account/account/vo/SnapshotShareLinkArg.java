package com.binance.mgs.account.account.vo;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import org.hibernate.validator.constraints.NotBlank;

import java.io.Serializable;

/**
 * Created by yangyang on 2019/10/18.
 */
@Data
public class SnapshotShareLinkArg implements Serializable {


    @ApiModelProperty(required = true, notes = "图片link唯一key")
    @NotBlank
    private String uploadUrlUniqueKey;

    @ApiModelProperty(required = true, notes = "分享码")
    @NotBlank
    private String agentCode;


}
