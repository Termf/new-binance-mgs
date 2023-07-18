package com.binance.mgs.nft.webex.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateWebMessage implements Serializable {

    private String personId;

    private String toPersonEmail;

    private String text;
}
