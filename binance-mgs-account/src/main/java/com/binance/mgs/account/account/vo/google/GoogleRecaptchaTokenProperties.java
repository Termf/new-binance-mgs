package com.binance.mgs.account.account.vo.google;

import lombok.Data;

import java.io.Serializable;
import java.util.Date;

@Data
public class GoogleRecaptchaTokenProperties implements Serializable {

    private static final long serialVersionUID = 7334596082614961726L;
    private boolean valid;

    private String invalidReason;

    private String hostname;

    private String action;

    private Date createTime;
}
