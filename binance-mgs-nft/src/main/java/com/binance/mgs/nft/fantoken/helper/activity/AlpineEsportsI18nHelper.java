package com.binance.mgs.nft.fantoken.helper.activity;

import com.binance.master.utils.StringUtils;
import com.binance.mgs.nft.fantoken.helper.FanTokenBTSHelper;
import com.binance.nft.fantoken.activity.response.alpine.esports.AlpineEsportsHomepageResponse;
import com.binance.nft.fantoken.activity.vo.alpine.esports.AlpineEsportsNftOpenStatusInfo;
import com.binance.platform.mgs.base.helper.BaseHelper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Objects;

@Component
@RequiredArgsConstructor
public class AlpineEsportsI18nHelper {

    private final BaseHelper baseHelper;
    private final FanTokenBTSHelper fanTokenBTSHelper;

    public void doAlpineEsportsHomepageResponse(AlpineEsportsHomepageResponse response) {

        if (Objects.nonNull(response) && Objects.nonNull(response.getJourneyInfo())) {
            response.getJourneyInfo().setJourneyHeadline(fanTokenBTSHelper.getMessageByKey(response.getJourneyInfo()
                    .getJourneyHeadlineKey(), baseHelper.getLanguage()));
            response.getJourneyInfo().setJourneySubtitle(fanTokenBTSHelper.getMessageByKey(response.getJourneyInfo()
                    .getJourneySubtitleKey(), baseHelper.getLanguage()));

            if (Objects.nonNull(response.getUserInfo()) && Objects.nonNull(response.getUserInfo().getOpenStatusInfo())
                    && Objects.nonNull(response.getUserInfo().getOpenStatusInfo().getPhysicalRewardInfo())) {
                // 存在实物奖励
                if (StringUtils.isNotBlank(response.getUserInfo().getOpenStatusInfo().getPhysicalRewardInfo().getRewardDescriptionKey())) {
                    response.getUserInfo().getOpenStatusInfo().getPhysicalRewardInfo().setRewardDescription(
                            fanTokenBTSHelper.getMessageByKey(response.getUserInfo().getOpenStatusInfo().getPhysicalRewardInfo().getRewardDescriptionKey(),
                                    baseHelper.getLanguage()));
                }
            }
        }
    }

    public void doAlpineEsportsNftOpenStatusInfo(AlpineEsportsNftOpenStatusInfo info) {

        if (Objects.nonNull(info.getPhysicalRewardInfo()) && StringUtils.isNotBlank(info.getPhysicalRewardInfo().getRewardDescriptionKey())) {
            info.getPhysicalRewardInfo().setRewardDescription(fanTokenBTSHelper.getMessageByKey(info.getPhysicalRewardInfo().getRewardDescriptionKey(),
                    baseHelper.getLanguage()));
        }
    }
}
