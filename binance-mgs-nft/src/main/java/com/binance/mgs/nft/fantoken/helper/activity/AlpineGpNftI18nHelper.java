package com.binance.mgs.nft.fantoken.helper.activity;

import com.binance.mgs.nft.fantoken.helper.FanTokenBTSHelper;
import com.binance.nft.fantoken.activity.vo.alpine.gpnft.AlpineGpNftMatchInfo;
import com.binance.nft.fantoken.activity.vo.common.FtNftInfo;
import com.binance.platform.mgs.base.helper.BaseHelper;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import java.util.Objects;

@Component
@RequiredArgsConstructor
public class AlpineGpNftI18nHelper {

    private final BaseHelper baseHelper;
    private final FanTokenBTSHelper fanTokenBTSHelper;

    public void doAlpineGpNftMatchInfo(AlpineGpNftMatchInfo matchInfo) {

        if (Objects.nonNull(matchInfo) && StringUtils.isNotBlank(matchInfo.getMatchTitleKey())) {
            matchInfo.setMatchTitle(fanTokenBTSHelper.getMessageByKey(matchInfo.getMatchTitleKey(), baseHelper.getLanguage()));
        }
    }

    public void doFtNftInfo(FtNftInfo nftInfo) {

        if (Objects.nonNull(nftInfo) && StringUtils.isNotBlank(nftInfo.getNftNameKey())) {
            nftInfo.setNftName(fanTokenBTSHelper.getMessageByKey(nftInfo.getNftNameKey(), baseHelper.getLanguage()));
        }
    }
}
