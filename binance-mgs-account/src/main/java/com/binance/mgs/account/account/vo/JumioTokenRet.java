package com.binance.mgs.account.account.vo;

import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.util.Date;

@Setter
@Getter
public class JumioTokenRet implements Serializable {

    private static final long serialVersionUID = 1990249975311898297L;

    private Date timestamp;

    private String authorizationToken;

    private String clientRedirectUrl;

    private String jumioIdScanReference;

    private String msg;
}
