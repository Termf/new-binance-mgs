package com.binance.mgs.account.api.vo;

import io.swagger.annotations.ApiModel;
import lombok.Data;

import java.util.List;

@ApiModel
@Data
public class QuerySubUserApiKeyWhitelistRet {

    private Long keyId;

    private List<String> symbols;

    private Long lastUpdateTime;
}
