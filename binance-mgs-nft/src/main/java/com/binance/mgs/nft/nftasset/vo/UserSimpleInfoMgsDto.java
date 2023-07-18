package com.binance.mgs.nft.nftasset.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@RequiredArgsConstructor
public class UserSimpleInfoMgsDto {

    private String userId;
    private String nickName;
    private String email;
    private String avatarUrl;
    private String phone;
    private String bannerUrl;
    private String bio;
}
