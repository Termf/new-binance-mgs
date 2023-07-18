package com.binance.mgs.nft.nftasset.vo;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

@Data
public class UserAssetBalanceArg implements Serializable {

    private String fiatName;
    private List<String> assetList;
    private Integer actionType;

}
