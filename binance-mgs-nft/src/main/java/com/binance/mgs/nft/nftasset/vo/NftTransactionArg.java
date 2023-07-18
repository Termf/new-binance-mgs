package com.binance.mgs.nft.nftasset.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import java.io.Serializable;
import java.util.Date;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NftTransactionArg implements Serializable {

    private Long userId;
    @Min(1L)
    private Integer page;
    @Max(30L)
    private Integer rows;
    private Date startTime;
    private Date endTime;
    private Long transactionId;
    private List<Integer> operations;
}
