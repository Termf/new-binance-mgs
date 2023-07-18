package com.binance.mgs.nft.nftasset.vo;

import com.binance.platform.openfeign.jackson.Long2String;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CollectionNftRenamingArg implements Serializable {

    @Long2String
    private List<Long> nftInfoId;
    @Long2String
    private Long fromCollectionId;
    @Long2String
    private Long toCollectionId;
    private String toCollectionName;
    private Integer category;
}
