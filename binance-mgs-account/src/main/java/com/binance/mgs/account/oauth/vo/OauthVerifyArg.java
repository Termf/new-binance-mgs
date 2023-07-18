package com.binance.mgs.account.oauth.vo;

import com.binance.platform.mgs.base.vo.CommonArg;
import lombok.Data;

@Data
public class OauthVerifyArg extends CommonArg {
    private static final long serialVersionUID = 7239084763396177777L;
    private String email;
    private String verifyCode;
}
