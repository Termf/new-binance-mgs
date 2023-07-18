package com.binance.mgs.account.account.vo;

import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;

@Getter
@Setter
public class GenerateSecretKeyRet implements Serializable {

    private static final long serialVersionUID = 1649013062006547093L;

    private String secretKey;

    private String qrcode;

}
