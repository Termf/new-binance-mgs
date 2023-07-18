package com.binance.mgs.nft.nftasset.vo;

import com.binance.platform.openfeign.jackson.BigDecimal2String;
import com.binance.platform.openfeign.jackson.Long2String;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.Date;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class NftEventVoRet {

    private String message;
    private String userId;
    private String userNickName;
    private Byte eventType;
    @BigDecimal2String
    private BigDecimal amount;
    private String asset;
    private Date createTime;
}
