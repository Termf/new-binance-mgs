package com.binance.mgs.nft.webex.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WebMessage implements Serializable {

    private String id;

    private String roomId;

    private String roomType;

    private String text;

    private String personId;

    private String personEmail;

    private String html;

    private List<String> mentionedPeople;

    private String created;
}
