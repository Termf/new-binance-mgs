package com.binance.mgs.nft.fantoken.helper;

import com.binance.master.utils.StringUtils;
import com.binance.nft.fantoken.response.worldcup.WorldCupCountryNftWallResponse;
import com.binance.nft.fantoken.response.worldcup.WorldCupDailyChallengeResponse;
import com.binance.nft.fantoken.response.worldcup.WorldCupLeaderboardRewardResponse;
import com.binance.nft.fantoken.response.worldcup.WorldCupMatchDayRewardClaimResponse;
import com.binance.nft.fantoken.response.worldcup.WorldCupOnboardingResponse;
import com.binance.nft.fantoken.vo.worldcup.WorldCupMatchDayInfo;
import com.binance.nft.fantoken.vo.worldcup.WorldCupMatchInfo;
import com.binance.nft.fantoken.vo.worldcup.WorldCupTeamInfo;
import com.binance.platform.mgs.base.helper.BaseHelper;
import lombok.RequiredArgsConstructor;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Component;

@SuppressWarnings("all")
@Component
@RequiredArgsConstructor
public class FanTokenWorldCupI18nHelper {

    private final BaseHelper baseHelper;
    private final FanTokenBTSHelper fanTokenBTSHelper;

    public void doWorldCupTeamInfo(WorldCupTeamInfo teamInfo) {

        if (null != teamInfo) {
            teamInfo.setWorldcupTeamName(fanTokenBTSHelper.getMessageByKey(teamInfo.getWorldcupTeamNameKey(),
                    baseHelper.getLanguage()));
        }
    }

    public void doWorldCupMatchInfo(WorldCupMatchInfo matchInfo) {

        if (null != matchInfo) {
            doWorldCupTeamInfo(matchInfo.getTeamAInfo());
            doWorldCupTeamInfo(matchInfo.getTeamBInfo());
        }
    }

    public void doWorldCupOnboardingResponse(WorldCupMatchDayInfo dayInfo) {

        if (null != dayInfo) {
            dayInfo.setMatchDayDesc(fanTokenBTSHelper.getMessageByKey(dayInfo.getMatchDayDescKey(), baseHelper.getLanguage()));
            if (CollectionUtils.isNotEmpty(dayInfo.getMatchInfos())) {
                dayInfo.getMatchInfos().forEach(this::doWorldCupMatchInfo);
            }
        }
    }

    public void doWorldCupDailyChallengeResponse(WorldCupDailyChallengeResponse response) {

        if (null != response) {
            if (CollectionUtils.isNotEmpty(response.getMatchDayInfos())) {
                response.getMatchDayInfos().forEach(this::doWorldCupOnboardingResponse);
            }
            if (null != response.getMatchDayInfo()) {
                doWorldCupOnboardingResponse(response.getMatchDayInfo());
            }
        }
    }

    public void doWorldCupCountryNftWallResponse(WorldCupCountryNftWallResponse response) {

        if (null != response && CollectionUtils.isNotEmpty(response.getNftInfos())) {
            response.getNftInfos().forEach(n -> n.setWorldcupTeamName(fanTokenBTSHelper.getMessageByKey(n.getWorldcupTeamNameKey(),
                    baseHelper.getLanguage())));
        }
    }
    
    public void doWorldCupMatchDayRewardClaimResponse(WorldCupMatchDayRewardClaimResponse response) {

        if (null != response && null != response.getRewardDetail()
                && CollectionUtils.isNotEmpty(response.getRewardDetail().getCountryNftInfos())) {
            response.getRewardDetail().getCountryNftInfos().forEach(n -> n.setWorldcupTeamName(
                    fanTokenBTSHelper.getMessageByKey(n.getWorldcupTeamNameKey(),
                            baseHelper.getLanguage())
            ));
        }
    }

    public void doWorldCupLeaderboardRewardResponse(WorldCupLeaderboardRewardResponse response) {

        if (null != response && null != response.getNftInfo()
                && StringUtils.isNotBlank(response.getNftInfo().getNftNameKey())) {

//            // 如果是 sponsor nft, 不需要翻译
//            if (response.getNftInfo().getIsSponsorNft()) {
//                response.getNftInfo().setEditionName(response.getNftInfo().getNftNameKey());
//                return;
//            }

            String nftNameKey = response.getNftInfo().getNftNameKey();
            response.getNftInfo().setEditionName(nftNameKey);
//            response.getNftInfo().setEditionName(fanTokenBTSHelper.getMessageByKey(nftNameKey, baseHelper.getLanguage()));
        }
    }

    public void doWorldCupOnboardingResponseForLeaderboardNftReward(WorldCupOnboardingResponse response) {

        if (null != response && null != response.getNftInfo()
                && StringUtils.isNotBlank(response.getNftInfo().getNftNameKey())) {

//            // 如果是 sponsor nft, 不需要翻译
//            if (response.getNftInfo().getIsSponsorNft()) {
//                response.getNftInfo().setEditionName(response.getNftInfo().getNftNameKey());
//                return;
//            }

            String nftNameKey = response.getNftInfo().getNftNameKey();
            response.getNftInfo().setEditionName(nftNameKey);
//            response.getNftInfo().setEditionName(fanTokenBTSHelper.getMessageByKey(nftNameKey, baseHelper.getLanguage()));
        }
    }
}
