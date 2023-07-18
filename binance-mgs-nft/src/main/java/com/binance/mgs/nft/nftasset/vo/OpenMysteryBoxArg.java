package com.binance.mgs.nft.nftasset.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;

import javax.validation.constraints.NotNull;
import java.io.Serializable;
import java.util.List;

@Data
@Builder
@RequiredArgsConstructor
@AllArgsConstructor
public class OpenMysteryBoxArg implements Serializable {

    @NotNull
    private String serialsNo;

    @NotNull
    private Integer number;

    private List<Long> nftIds;
}
