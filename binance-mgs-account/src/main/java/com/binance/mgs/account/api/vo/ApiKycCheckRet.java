package com.binance.mgs.account.api.vo;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

@Data
public class ApiKycCheckRet {
    @ApiModelProperty(required = true, name = "是否通过中级(包含)以上的认证")
    private boolean isPass;
}
