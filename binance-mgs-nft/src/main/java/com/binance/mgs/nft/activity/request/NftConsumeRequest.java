package com.binance.mgs.nft.activity.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.Date;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NftConsumeRequest implements Serializable {
    /**
     * userId
     */
    private Long userId;
    /**
     * 领取Code
     */
    private String code;
    /**
     时间戳
     */
    private Date timestamp;

    private List<Long> subActivityCode;
}
