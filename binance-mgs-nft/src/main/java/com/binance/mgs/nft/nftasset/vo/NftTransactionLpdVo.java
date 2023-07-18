package com.binance.mgs.nft.nftasset.vo;

import com.binance.platform.openfeign.jackson.BigDecimal2String;
import com.binance.platform.openfeign.jackson.Long2String;
import lombok.*;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NftTransactionLpdVo implements Serializable {

    private Integer quantity;
    @Long2String
    private Long transactionId;
    @Long2String
    private Long nftId;
    private Integer operation;
    private byte nftType;
    private String network;
    @Deprecated
    private String source;
    private Date createTime;
    private List<String> zippedUrl;
    private String eventName;
    private String pageLink;
    @BigDecimal2String
    private BigDecimal amount;
    private String asset;
    private String remark;

    /**
     * for history inbox
     *
     * false:nothing  true:unread
     */
    private Boolean unreadFlag;

}
