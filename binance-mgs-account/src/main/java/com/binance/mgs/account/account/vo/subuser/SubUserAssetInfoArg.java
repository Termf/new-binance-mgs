package com.binance.mgs.account.account.vo.subuser;

import javax.validation.constraints.NotNull;

import lombok.Data;

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
