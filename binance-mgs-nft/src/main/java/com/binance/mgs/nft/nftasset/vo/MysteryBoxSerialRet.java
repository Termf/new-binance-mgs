package com.binance.mgs.nft.nftasset.vo;

import com.binance.platform.openfeign.jackson.Long2String;
import lombok.Data;

import java.io.Serializable;

@Data
public class MysteryBoxSerialRet implements Serializable {

    private String serialsNo;
    private String serialTitle;
    private String serialUrl;
    private String contractAddress;
    private Integer quantity;
    @Long2String
    private Long merchantId;
    private String merchantName;
    private String merchantAvatar;

}
