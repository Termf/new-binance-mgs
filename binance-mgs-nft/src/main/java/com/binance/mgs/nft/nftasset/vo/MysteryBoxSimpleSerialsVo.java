package com.binance.mgs.nft.nftasset.vo;

import com.binance.nft.assetservice.api.data.dto.SpecificationDto;
import com.binance.platform.openfeign.jackson.Long2String;
import lombok.Builder;
import lombok.Data;

import java.io.Serializable;
@Data
@Builder
public class MysteryBoxSimpleSerialsVo implements Serializable {

    @Long2String
    private Long serialsNo;
    private String serialsName;
    private String zippedUrl;
    private Integer quantity;
    private String network;

}
