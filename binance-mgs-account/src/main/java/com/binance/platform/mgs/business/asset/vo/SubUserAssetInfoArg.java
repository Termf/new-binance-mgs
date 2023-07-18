package com.binance.platform.mgs.business.asset.vo;

import lombok.Data;

import javax.validation.constraints.NotNull;

/**
 * Created by Fei.Huang on 2018/11/7.
 */
@Data
public class SubUserAssetInfoArg {
    @NotNull
    private Integer page;
    @NotNull
    private Integer rows;
    private String email;
    private String isSubUserEnabled;
}
