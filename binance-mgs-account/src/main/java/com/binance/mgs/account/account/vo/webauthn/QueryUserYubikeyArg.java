package com.binance.mgs.account.account.vo.webauthn;

import com.binance.platform.mgs.base.vo.CommonArg;
import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class QueryUserYubikeyArg extends CommonArg {

    @ApiModelProperty("Origin")
    private String origin;

}
