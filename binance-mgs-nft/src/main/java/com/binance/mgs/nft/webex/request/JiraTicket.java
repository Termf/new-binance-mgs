package com.binance.mgs.nft.webex.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class JiraTicket implements Serializable {

    private String title;

    private String url;

    private String email;

}
