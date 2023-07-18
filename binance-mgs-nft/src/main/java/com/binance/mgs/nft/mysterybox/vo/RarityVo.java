package com.binance.mgs.nft.mysterybox.vo;

import lombok.Builder;
import lombok.Data;

import java.io.Serializable;

@Data
@Builder
public class RarityVo implements Serializable {

    private int value;

    private String name;
}
