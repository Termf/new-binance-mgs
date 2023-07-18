package com.binance.mgs.account.oauth.vo;

import com.binance.platform.mgs.base.vo.CommonArg;
import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
public class OauthCallbackArg extends CommonArg {
    private static final long serialVersionUID = 5287002017192783969L;
    private String code;
    private String state;
}
