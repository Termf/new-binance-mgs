package com.binance.mgs.nft.webex.request;

import lombok.Builder;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

@Data
@Builder
public class WebexDataVo implements Serializable {

    private String id;

    private String roomId;

    private String roomType;

    private String personId;

    private String personEmail;

    private List<String> mentionedPeople;

    private String created;
}
