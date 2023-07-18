package com.binance.mgs.account.account.vo.subuser;


import lombok.Data;

import javax.validation.constraints.NotBlank;

/**
 * Created by Fei.Huang on 2018/11/28.
 */
@Data
public class SubAccQueryTradeDetailsArg extends QueryTradeDetailsArg {

    private static final long serialVersionUID = -3364845045470435617L;
    @NotBlank
    private String userId;
}
