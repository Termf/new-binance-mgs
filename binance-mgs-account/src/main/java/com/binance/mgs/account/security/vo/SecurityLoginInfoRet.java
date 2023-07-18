package com.binance.mgs.account.security.vo;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class SecurityLoginInfoRet {

    @ApiModelProperty("用户是否存在")
    private Boolean valid;

    @ApiModelProperty("用户禁登录")
    private Boolean disable;
}
