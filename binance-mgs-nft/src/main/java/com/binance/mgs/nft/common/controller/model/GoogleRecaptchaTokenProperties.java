package com.binance.mgs.nft.common.controller.model;

import lombok.Data;

import java.io.Serializable;
import java.util.Date;

@Data
public class GoogleRecaptchaTokenProperties implements Serializable {

    private boolean valid;

    private String invalidReason;

    private String hostname;

    private String action;

    private Date createTime;
}
