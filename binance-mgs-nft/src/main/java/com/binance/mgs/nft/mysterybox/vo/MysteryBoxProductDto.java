package com.binance.mgs.nft.mysterybox.vo;

import com.binance.nft.mystery.api.vo.MysteryBoxProductVo;
import com.binance.nft.tradeservice.dto.LaunchpadConfigDto;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;

import java.util.Date;

@Data
public class MysteryBoxProductDto extends MysteryBoxProductVo {

    private LaunchpadConfigDto dto;

    private String title;

    private String pageLink;

    private Date compareStartTime;
    /**
     * 1： 盲盒，2：launchpad
     */
    private Integer configType = 1;

    private Date timestamp;

}
