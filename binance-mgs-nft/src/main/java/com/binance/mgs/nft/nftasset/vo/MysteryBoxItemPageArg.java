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
public class MysteryBoxItemPageArg implements Serializable {


    private Integer page;

    private Integer pageSize;
    @NotNull
    private Long serialsNo;

}
