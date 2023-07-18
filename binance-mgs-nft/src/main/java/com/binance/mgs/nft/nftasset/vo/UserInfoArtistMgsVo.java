package com.binance.mgs.nft.nftasset.vo;

import lombok.Data;

@Data
public class UserInfoArtistMgsVo extends UserInfoMgsVo{
    private String userId;

    private boolean artist;
}
