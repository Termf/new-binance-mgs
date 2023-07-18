package com.binance.mgs.nft.activity.response;

import com.binance.nft.activityservice.vo.DistributeHistoryDetailVo;
import lombok.Data;

@Data
public class NftDistributeHisVo {
    private Long id;
    private String time;
    private String itemUrl;
    private String network;
    private String eventName;
    private Integer nftType;
    private Integer quantity;
    private Integer status;
    private String expireTime;
    private DistributeHistoryDetailVo detail;
    private Boolean unreadFlag;
}
