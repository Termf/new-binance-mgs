package com.binance.mgs.account.authcenter.vo;

import com.binance.platform.mgs.base.vo.CommonArg;
import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.validation.constraints.NotBlank;

@EqualsAndHashCode(callSuper = true)
@Data
public class LoginCallbackArg extends CommonArg {
    @NotBlank
    private String code;
    private String callback;
}
