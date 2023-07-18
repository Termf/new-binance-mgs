package com.binance.mgs.nft.trade.response;

import com.binance.nft.tradeservice.vo.UserInfoVo;
import lombok.Data;

@Data
public class ArtistUserMgsInfo extends UserInfoVo {

    private String userId;

    private boolean artist;
}
