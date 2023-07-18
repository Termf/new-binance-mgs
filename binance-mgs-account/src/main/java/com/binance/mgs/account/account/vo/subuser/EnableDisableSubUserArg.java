package com.binance.mgs.account.account.vo.subuser;

import lombok.Data;

import javax.validation.constraints.NotNull;

/**
 * Created by Fei.Huang on 2018/11/7.
 */
@Data
public class EnableDisableSubUserArg {
    @NotNull
    String[] subUserIds;
}
