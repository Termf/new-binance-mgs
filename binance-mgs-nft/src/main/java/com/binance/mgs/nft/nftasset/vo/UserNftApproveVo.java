package com.binance.mgs.nft.nftasset.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;

import javax.validation.constraints.NotNull;
import java.io.Serializable;

@Data
@Builder
@RequiredArgsConstructor
@AllArgsConstructor
public class UserNftApproveVo implements Serializable {

    @NotNull
    private Long nftInfoId;
    @NotNull
    private Integer status;
}