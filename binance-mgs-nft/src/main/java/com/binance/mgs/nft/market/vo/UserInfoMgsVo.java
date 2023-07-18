package com.binance.mgs.nft.market.vo;

import lombok.Builder;
import lombok.Data;
import java.io.Serializable;

@Data
public class UserInfoMgsVo implements Serializable {

    private String userId;

    private String avatarUrl;

    private String nickName;

    private boolean artist;
}