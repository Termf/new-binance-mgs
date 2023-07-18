package com.binance.mgs.account.account.vo;

import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;

@Getter
@Setter
public class UrlAndProjectRet implements Serializable {

    private static final long serialVersionUID = 8430057735701325145L;

    private String server;

    private String web;
}
