package com.binance.mgs.account.account.vo.userpersonalconfig.response;

import io.swagger.annotations.ApiModel;
import lombok.Data;

import java.util.List;

@Data
@ApiModel
public class UnifiedBatchQueryUserPersonalConfigRet {

    private List<UnifiedQueryUserPersonalConfigRet> configRets;
}
