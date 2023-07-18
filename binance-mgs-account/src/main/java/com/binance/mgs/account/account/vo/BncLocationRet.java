package com.binance.mgs.account.account.vo;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

/**
 * @author rudy.c
 * @date 2022-05-05 17:58
 */
@Data
public class BncLocationRet {
    @ApiModelProperty(value = "bnc-location")
    private String bncLocation;
}
