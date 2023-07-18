package com.binance.mgs.account.account.vo.webauthn;

import com.binance.platform.mgs.base.vo.CommonArg;
import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.validator.constraints.NotBlank;

@Getter
@Setter
public class OriginArg extends CommonArg {


    @ApiModelProperty("origin")
    @NotBlank
    private String origin;

}
