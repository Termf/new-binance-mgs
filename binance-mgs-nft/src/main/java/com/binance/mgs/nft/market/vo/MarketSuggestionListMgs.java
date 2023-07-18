package com.binance.mgs.nft.market.vo;

import com.binance.nft.market.vo.MarketSuggestionList;
import com.binance.platform.openfeign.jackson.Long2String;
import lombok.Builder;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

@Data
@Builder
public class MarketSuggestionListMgs implements Serializable {

    List<com.binance.nft.market.vo.MarketSuggestionList.NFTCollections> nftCollectionList;

    List<MarketSuggestionList.NFTItems> nftItemList;

    List<NFTCreators> nftCreatorsList;

    List<com.binance.nft.market.vo.MarketSuggestionList.MysteryBoxCollections> mysteryCollectionList;

    @Builder
    @Data
    public static class NFTCollections {

        @Long2String
        private Long id;

        @Long2String
        private Long parentId;

        private String parentName;

        @Long2String
        private Long layerId;

        private String layerName;

        private String avatorUrl = "";
    }

    @Builder
    @Data
    public static class NFTCreators {

        @Long2String
        private Long product_id;

        @Long2String
        private Long product_no;

//        @Long2String
        private String creator_id;

        private String creator_name;

        private String creator_name_norm;

        //deprecated
        private String avatorUrl = "";

        private String avatarUrl;

        @Long2String
        private Long followerCount;

        /**
         * 0:not follow，1:followed，2:self
         */
        private Integer followRelation;
    }

    @Builder
    @Data
    public static class MysteryBoxCollections {

        @Long2String
        private Long productId;

        @Long2String
        private Long product_no;

        private String title;

        private String title_norm;

        @Long2String
        private Long serialsNo;

        private String avatorUrl = "";
    }
}

