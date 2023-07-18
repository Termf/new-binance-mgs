package com.binance.mgs.nft.market.vo;

import com.binance.nft.market.vo.homepage.Subtitle;
import com.fasterxml.jackson.annotation.JsonIgnore;
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
public class TopicMgs implements Serializable {

    private List<TopicItemMgs> items;

    private String title;

    private Subtitle subtitle;

    @JsonIgnore
    private Long topicId;


}
