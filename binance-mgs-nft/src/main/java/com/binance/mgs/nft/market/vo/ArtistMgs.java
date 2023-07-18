package com.binance.mgs.nft.market.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;

import java.io.Serializable;

@Data
@Builder
@AllArgsConstructor
@RequiredArgsConstructor
public class ArtistMgs implements Serializable {

    private String name;

    private String avatar;

    private boolean artist;

    private String userId;
}

