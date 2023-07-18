package com.binance.mgs.account.account.vo.subuser;

import lombok.Data;

import javax.validation.constraints.NotNull;

/**
 * Created by Fei.Huang on 2018/11/12.
 */
@Data
public class SubUserAssetDetailsArg {
    @NotNull
    private String subUserId;
    private String asset;
}
