package com.binance.mgs.nft.webex.request;

import lombok.Data;

import java.io.Serializable;

@Data
public class WebexRequest implements Serializable {

    private String id;

    private String name;

    private String targetUrl;

    private String resource;

    private String event;

    private String filter;

    private String orgId;

    private String createdBy;

    private String appId;

    private String ownedBy;

    private String status;

    private String created;

    private String actorId;

    private WebexDataVo data;
}
