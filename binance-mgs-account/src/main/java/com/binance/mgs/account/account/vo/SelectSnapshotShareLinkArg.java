package com.binance.mgs.account.account.vo;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import org.hibernate.validator.constraints.NotBlank;

import java.io.Serializable;

/**
 * Created by yangyang on 2019/10/18.
 */
@Data
public class SelectSnapshotShareLinkArg implements Serializable {


    @ApiModelProperty(required = true, notes = "图片link-id")
    @NotBlank
    private String id;
}
