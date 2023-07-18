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
public class MysteryBoxPageArg implements Serializable {
    @NotNull
    private Integer page;
    @NotNull
    private Integer pageSize;

    private Integer number;
    private Byte openStatus;
}
