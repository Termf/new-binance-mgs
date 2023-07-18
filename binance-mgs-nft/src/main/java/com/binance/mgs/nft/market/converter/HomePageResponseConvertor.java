package com.binance.mgs.nft.market.converter;

import com.binance.master.utils.CopyBeanUtils;
import com.binance.mgs.nft.market.utils.AesUtil;
import com.binance.mgs.nft.market.vo.ArtistMgs;
import com.binance.mgs.nft.market.vo.HomePageMgsResponse;
import com.binance.mgs.nft.market.vo.TopicItemMgs;
import com.binance.mgs.nft.market.vo.TopicMgs;
import com.binance.nft.market.vo.homepage.HomePageResponse;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;


public class HomePageResponseConvertor {
    public static HomePageMgsResponse convert(HomePageResponse homePageResponse, String password) {
        HomePageMgsResponse response = new HomePageMgsResponse();
        response.setBanners(homePageResponse.getBanners());
        List<TopicMgs> topicList = new ArrayList<>(homePageResponse.getTopics().size());
        response.setTopics(topicList);

        homePageResponse.getTopics().forEach(topics -> {
            if(topics.getItems() == null) {
                topics.setItems(Collections.emptyList());
            }
            TopicMgs topicMgs = CopyBeanUtils.fastCopy(topics, TopicMgs.class);
            List<TopicItemMgs> topicItemMgsList = new ArrayList<>(topics.getItems().size());

            topicMgs.setItems(topicItemMgsList);

            topics.getItems().forEach(topicItem -> {
                TopicItemMgs topicItemMgs = CopyBeanUtils.fastCopy(topicItem, TopicItemMgs.class);
                topicItemMgs.setApprove(topicItem.getApprove());

                if (topicItem.getArtist() != null){
                    ArtistMgs artistMgs = CopyBeanUtils.fastCopy(topicItem.getArtist(), ArtistMgs.class);

                    if (topicItem.getArtist().getUserId() != null){
                        artistMgs.setUserId(AesUtil.encrypt(topicItem.getArtist().getUserId().toString(), password));
                    }
                    topicItemMgs.setArtist(artistMgs);
                }
                topicItemMgs.setTimestamp(LocalDateTime.now().toInstant(ZoneOffset.ofHours(0)).toEpochMilli());
                topicItemMgsList.add(topicItemMgs);
            });
            topicList.add(topicMgs);
        });


        return response;
    }
}
