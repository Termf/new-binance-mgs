package com.binance.mgs.nft.fanverse.helper;

import com.binance.mgs.nft.fantoken.helper.FanTokenBTSHelper;
import com.binance.nft.fantoken.activity.response.bws.onboarding.BwsOnboardingUserResponse;
import com.binance.nft.fantoken.activity.response.partner.FanTokenPartnerTeamInfo;
import com.binance.nft.fantoken.activity.vo.bws.onboarding.BwsMerchantInfo;
import com.binance.nft.fantoken.activity.vo.bws.onboarding.BwsPassportDescriptionInfo;
import com.binance.nft.fantoken.activity.vo.bws.onboarding.BwsPassportInfo;
import com.binance.nft.fantoken.activity.vo.bws.utility.BwsPredictCampaignInfo;
import com.binance.nft.fantoken.activity.vo.bws.utility.BwsPredictMatchInfo;
import com.binance.nft.fantoken.activity.vo.bws.utility.BwsPredictTeamInfo;
import com.binance.nft.fantoken.activity.vo.bws.utility.BwsVoteInfo;
import com.binance.nft.fantoken.activity.vo.common.FtNftInfo;
import com.binance.nft.fantoken.activity.vo.partner.PartnerTaskInfo;
import com.binance.nft.fantoken.activity.vo.referral.ReferralEventInfo;
import com.binance.platform.mgs.base.helper.BaseHelper;
import lombok.RequiredArgsConstructor;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Component;

import java.util.Objects;

@Component
@RequiredArgsConstructor
public class FanVerseI18nHelper {

    private final BaseHelper baseHelper;
    private final FanTokenBTSHelper fanTokenBTSHelper;

    public void doBwsMerchantInfo(BwsMerchantInfo info) {

        if (Objects.nonNull(info)) {
            info.setMerchantName(fanTokenBTSHelper.getMessageByKey(info.getMerchantNameKey(), baseHelper.getLanguage()));
        }
    }

    public void doOnboardingUserInfo(BwsOnboardingUserResponse response) {

        if (Objects.nonNull(response) && Objects.nonNull(response.getMerchantInfo())) {
            response.getMerchantInfo().setMerchantName(fanTokenBTSHelper.getMessageByKey(
                    response.getMerchantInfo().getMerchantNameKey(), baseHelper.getLanguage()));
            doOnboardingPassportInfo(response.getMerchantInfo().getExperiencePassportInfo());
            doOnboardingPassportInfo(response.getMerchantInfo().getSuperPassportInfo());
        }
    }

    public void doOnboardingPassportInfo(BwsPassportInfo info) {

        if (Objects.nonNull(info)) {
            info.setName(fanTokenBTSHelper.getMessageByKey(info.getNameKey(), baseHelper.getLanguage()));
            if (CollectionUtils.isNotEmpty(info.getDescriptionInfos())) {
                info.getDescriptionInfos().forEach(this::doOnboardingPassportDescriptionInfo);
            }
        }
    }

    public void doOnboardingPassportDescriptionInfo(BwsPassportDescriptionInfo info) {

        if (Objects.nonNull(info)) {
            info.setText(fanTokenBTSHelper.getMessageByKey(info.getTextKey(), baseHelper.getLanguage()));
        }
    }

    public void doBwsVoteInfo(BwsVoteInfo info) {

        if (Objects.nonNull(info)) {
            info.setVoteName(fanTokenBTSHelper.getMessageByKey(info.getVoteNameKey(), baseHelper.getLanguage()));
            info.setVoteDesc(fanTokenBTSHelper.getMessageByKey(info.getVoteDescKey(), baseHelper.getLanguage()));
            // option
            if (CollectionUtils.isNotEmpty(info.getOptionInfos())) {
                info.getOptionInfos().forEach(o -> {
                    o.setOptionName(fanTokenBTSHelper.getMessageByKey(o.getOptionNameKey(), baseHelper.getLanguage()));
                    o.setOptionDesc(fanTokenBTSHelper.getMessageByKey(o.getOptionDescKey(), baseHelper.getLanguage()));
                });
            }
            // update
            if (CollectionUtils.isNotEmpty(info.getUpdateInfos())) {
                info.getUpdateInfos().forEach(u -> {
                    u.setUpdateEventTitle(fanTokenBTSHelper.getMessageByKey(u.getUpdateEventTitleKey(), baseHelper.getLanguage()));
                    u.setUpdateEventContent(fanTokenBTSHelper.getMessageByKey(u.getUpdateEventContentKey(), baseHelper.getLanguage()));
                });
            }
        }
    }

    public void doBwsPredictTeamInfo(BwsPredictTeamInfo teamInfo) {
        if (Objects.nonNull(teamInfo)) {
            teamInfo.setTeamName(fanTokenBTSHelper.getMessageByKey(teamInfo.getTeamNameKey(), baseHelper.getLanguage()));
        }
    }

    public void doBwsPredictMatchInfo(BwsPredictMatchInfo matchInfo) {
        if (Objects.nonNull(matchInfo)) {
            matchInfo.setMatchDescription(fanTokenBTSHelper.getMessageByKey(matchInfo.getMatchDescriptionKey(), baseHelper.getLanguage()));
            doBwsPredictTeamInfo(matchInfo.getTeamAInfo());
            doBwsPredictTeamInfo(matchInfo.getTeamBInfo());
        }
    }

    public void doBwsPredictCampaignInfo(BwsPredictCampaignInfo campaignInfo) {
        if (Objects.nonNull(campaignInfo)){
            campaignInfo.setCampaignTitle(fanTokenBTSHelper.getMessageByKey(campaignInfo.getCampaignTitleKey(), baseHelper.getLanguage()));
            if (CollectionUtils.isNotEmpty(campaignInfo.getMatchInfos())) {
                campaignInfo.getMatchInfos().forEach(this::doBwsPredictMatchInfo);
            }
        }
    }

    public void doPartnerTeamInfo(FanTokenPartnerTeamInfo fanTokenPartnerTeamInfo) {
        if (Objects.nonNull(fanTokenPartnerTeamInfo)) {
            fanTokenPartnerTeamInfo.setPartnerName(fanTokenBTSHelper.getMessageByKey(fanTokenPartnerTeamInfo.getPartnerNameKey(), baseHelper.getLanguage()));
            fanTokenPartnerTeamInfo.setPartnerDescription(fanTokenBTSHelper.getMessageByKey(fanTokenPartnerTeamInfo.getPartnerDescriptionKey(), baseHelper.getLanguage()));
            doFtNftInfo(fanTokenPartnerTeamInfo.getNftInfo());
        }
    }

    public void doFtNftInfo(FtNftInfo ftNftInfo) {
        if (Objects.nonNull(ftNftInfo)) {
            ftNftInfo.setNftName(fanTokenBTSHelper.getMessageByKey(ftNftInfo.getNftNameKey(), baseHelper.getLanguage()));
        }
    }

    public void doPartnerTaskInfo(PartnerTaskInfo partnerTaskInfo) {
        if (Objects.nonNull(partnerTaskInfo)) {
            partnerTaskInfo.setPartnerTaskName(fanTokenBTSHelper.getMessageByKey(partnerTaskInfo.getPartnerTaskNameKey(), baseHelper.getLanguage()));
        }
    }

    public void doReferralEventInfo(ReferralEventInfo referralEventInfo) {
        if (Objects.nonNull(referralEventInfo)) {
            referralEventInfo.setInviteEventName(fanTokenBTSHelper.getMessageByKey(referralEventInfo.getInviteEventNameKey(), baseHelper.getLanguage()));
            referralEventInfo.setInviteEventDesc(fanTokenBTSHelper.getMessageByKey(referralEventInfo.getInviteEventDescKey(), baseHelper.getLanguage()));
        }
    }
}
