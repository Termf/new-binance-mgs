package com.binance.mgs.account.api.vo;

import io.swagger.annotations.ApiModel;
import lombok.Data;

@Data
@ApiModel("ApiManageIpConfigRet")
public class ApiManageIpConfigRet {

    /**
     * 主键
     */
    private Long id;

    /**
     * 第三方名称
     */
    private String apiManageIpConfigName;
}
