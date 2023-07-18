package com.binance.mgs.nft.nftasset.vo;

import com.binance.nft.assetservice.api.data.dto.SpecificationDto;
import com.binance.platform.openfeign.jackson.Long2String;
import lombok.Builder;
import lombok.Data;

import java.io.Serializable;

@Data
@Builder
public class MysteryBoxSimpleItemVo implements Serializable {

    private String nftTitle;
    private String itemUrl;
    @Long2String
    private Long serialsNo;
    @Long2String
    private Long itemId;
    private Integer itemQty;
    private Byte rarity;
    private Byte assetStatus;
    private Byte marketStatus;
    @Long2String
    private Long secondMarketSellingDelay;
    @Long2String
    private Long duration;
    private SpecificationDto specificationDto;

    private String freezeReasonId;
    private String freezeReason;

    public void setForbiddenState(Integer forbiddenState) {
        this.specificationDto = SpecificationDto.builder().build().initForbiddenState(forbiddenState);
    }
}
