package com.binance.mgs.nft.nftasset.vo;

import com.binance.platform.openfeign.jackson.BigDecimal2String;
import com.binance.platform.openfeign.jackson.Long2String;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MysteryBoxMgsVo {
    private String name;

    @Long2String
    private Long serialsNo;

    private List<SeriesMgsVo> series;

    private Integer store;

    private Date startTime;

    private Date endTime;

    @Long2String
    private Long secondMarketSellingDelay;

    private String aboutArtistVideo;

    private String network;

    private String image;

    @BigDecimal2String
    private BigDecimal price;

    private String currency;

    private String artist;

    private UserInfoMgsVo creator;
}
