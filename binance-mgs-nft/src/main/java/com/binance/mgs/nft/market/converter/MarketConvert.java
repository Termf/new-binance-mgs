package com.binance.mgs.nft.market.converter;

import com.binance.master.utils.StringUtils;
import com.binance.mgs.nft.market.vo.MarketProductReq;
import com.binance.nft.market.request.MarketProductRequest;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.Arrays;
import java.util.Optional;

@Mapper(componentModel = "spring", imports = {Arrays.class, Optional.class, StringUtils.class})
public interface MarketConvert {
    @Mapping(target = "tradeType", expression = "java(Optional.ofNullable(marketProductReq.getTradeType()).map(t->Arrays.asList(t)).orElse(null))")
    @Mapping(target = "mediaType", expression = "java(StringUtils.isBlank(marketProductReq.getMediaType()) ? null : Arrays.asList(marketProductReq.getMediaType()))")
    MarketProductRequest convertProductList(MarketProductReq marketProductReq);
}
