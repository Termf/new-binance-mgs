package com.binance.mgs.account.api.vo;

import com.google.common.collect.Lists;
import io.swagger.annotations.ApiModel;
import lombok.Data;

import java.util.List;

@Data
@ApiModel("AllApiManageIpConfigRet")
public class AllApiManageIpConfigRet {

    private List<ApiManageIpConfigRet> apiManageIpConfigRets = Lists.newArrayList();
}
