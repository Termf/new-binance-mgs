package com.binance.mgs.nft.market.vo;

import com.binance.nft.market.vo.homepage.Banner;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;

import java.io.Serializable;
import java.util.List;

@Data
@Builder
@AllArgsConstructor
@RequiredArgsConstructor
public class HomePageMgsResponse implements Serializable {

    private List<Banner> banners;

    private List<TopicMgs> topics;
}

