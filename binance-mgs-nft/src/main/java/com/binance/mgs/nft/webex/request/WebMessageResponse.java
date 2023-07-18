package com.binance.mgs.nft.webex.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class WebMessageResponse implements Serializable {

    private String id;

    private String roomId;

    private String toPersonEmail;

    private String roomType;

    private String text;

    private String personId;

    private String personEmail;

    private String created;
}
