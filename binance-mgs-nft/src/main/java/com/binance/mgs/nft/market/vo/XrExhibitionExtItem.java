package com.binance.mgs.nft.market.vo;

import com.binance.nft.market.vo.xrspace.XrExhibitionListVo;
import com.binance.nft.tradeservice.vo.UserInfoVo;
import lombok.Data;

@Data
public class XrExhibitionExtItem extends XrExhibitionListVo.XrExhibitionItem {

    private Long productId;

    private String title;

    private String description;

    private UserInfoVo creator;

    private UserInfoVo owner;

    private String url;

    private Long approveCount;

    private String rawUrl;

    private String originRawUrl;

    private String originCoverUrl;

    private String mediaType;

    private Integer rawSize;

    private String specification;

    private Long duration;
}
