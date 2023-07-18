package com.binance.mgs.nft.nftasset.vo;

import com.binance.nft.tradeservice.vo.UserInfoVo;
import lombok.Data;

@Data
public class ArtistUserInfoMgs extends UserInfoVo {

    private String userId;

    private boolean artist;
}
