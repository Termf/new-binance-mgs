package com.binance.mgs.nft.mysterybox.helper;

import com.binance.master.utils.StringUtils;
import com.binance.mgs.nft.fantoken.helper.FanTokenBTSHelper;
import com.binance.nft.fantoken.profile.api.vo.TagInfoVO;
import com.binance.nft.fantoken.request.fanshop.AcceptOrderRequest;
import com.binance.nft.fantoken.response.fanshop.FanShopCheckoutResponse;
import com.binance.nft.fantoken.response.fanshop.FanShopFiatCheckoutResponse;
import com.binance.nft.fantoken.response.fanshop.FanShopItemResponse;
import com.binance.nft.fantoken.response.fanshop.ItemWithTeamInfo;
import com.binance.nft.fantoken.response.fanshop.OrderDetailResponse;
import com.binance.nft.fantoken.response.fanshop.SimpleItemInfo;
import com.binance.nft.fantoken.response.nftstaking.CalculationHistoryResponse;
import com.binance.nft.fantoken.response.nftstaking.DistributionHistoryResponse;
import com.binance.nft.fantoken.response.nftstaking.NftStakingDisplayResponse;
import com.binance.nft.fantoken.response.nftstaking.StakingHistoryResponse;
import com.binance.nft.fantoken.response.nftstaking.UsableNftResponse;
import com.binance.nft.fantoken.response.singleticket.SingleTicketCardDetailPageResponse;
import com.binance.nft.fantoken.response.ticketnft.TicketNftEventCardDetailPageResponse;
import com.binance.nft.fantoken.vo.BannerVO;
import com.binance.nft.fantoken.vo.CategoryDisplayVO;
import com.binance.nft.fantoken.vo.SimpleTeamInfo;
import com.binance.nft.fantoken.vo.TeamVO;
import com.binance.nft.fantoken.vo.VoteUpdateInfoVO;
import com.binance.nft.fantoken.vo.VoteVO;
import com.binance.nft.fantoken.vo.fanprofile.FanCollectionVO;
import com.binance.nft.fantoken.vo.mvpvote.LeaderBoard;
import com.binance.nft.fantoken.vo.mvpvote.MvpVoteConfigVO;
import com.binance.nft.fantoken.vo.mvpvote.MvpVoteUpdateVO;
import com.binance.nft.fantoken.vo.mvpvote.MvpVotingDetailVO;
import com.binance.nft.fantoken.vo.mvpvote.PlayerVoteDetailVO;
import com.binance.nft.fantoken.vo.mvpvote.PlayerVoteInfo;
import com.binance.nft.fantoken.vo.mvpvote.PlayerVoteOptionDetailVO;
import com.binance.nft.fantoken.vo.mvpvote.TeamPlayerVO;
import com.binance.nft.fantoken.vo.mvpvote.VotingPollsVO;
import com.binance.nft.fantoken.vo.poap.BranchVenueVO;
import com.binance.nft.fantoken.vo.poap.CampaignPieceInfo;
import com.binance.nft.fantoken.vo.poap.MainVenueVO;
import com.binance.nft.fantoken.vo.prediction.InviterPosterVO;
import com.binance.nft.fantoken.vo.prediction.PredictionBaseInfo;
import com.binance.nft.fantoken.vo.prediction.PredictionCampaignBaseInfo;
import com.binance.nft.fantoken.vo.prediction.PredictionCampaignEventInfo;
import com.binance.nft.fantoken.vo.prediction.PredictionCampaignInfo;
import com.binance.nft.fantoken.vo.prediction.PredictionCampaignLeaderboardInfo;
import com.binance.nft.fantoken.vo.prediction.PredictionCampaignRewardsInfo;
import com.binance.nft.fantoken.vo.prediction.PredictionCampaignUserInfo;
import com.binance.nft.fantoken.vo.prediction.PredictionInfo;
import com.binance.nft.fantoken.vo.prediction.PredictionProfileInfo;
import com.binance.nft.fantoken.vo.prediction.PredictionRewardsVO;
import com.binance.nft.fantoken.vo.prediction.PredictionTeamVO;
import com.binance.nft.fantoken.vo.prediction.PredictionUserRewardsInfo;
import com.binance.nft.fantoken.vo.singleticket.SingleMatchInfo;
import com.binance.nft.fantoken.vo.singleticket.SingleTicketCardRewardVO;
import com.binance.nft.fantoken.vo.ticketnft.TicketNftBenefitInfo;
import com.binance.nft.fantoken.vo.ticketnft.TicketNftCardBenefitVO;
import com.binance.nft.fantoken.vo.ticketnft.TicketNftCardDetailVO;
import com.binance.nft.fantoken.vo.ticketnft.TicketNftCardInformationVO;
import com.binance.nft.fantoken.vo.ticketnft.TicketNftEventInfo;
import com.binance.platform.mgs.base.helper.BaseHelper;
import lombok.RequiredArgsConstructor;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class FanTokenI18nHelper {

    private final BaseHelper baseHelper;
    private final FanTokenBTSHelper fanTokenBTSHelper;

    public void doTeamI18n(TeamVO teamVO) {

        if (null == teamVO) {
            return;
        }

        teamVO.setTeamName(fanTokenBTSHelper.getMessageByKey(teamVO.getTeamNameKey(), baseHelper.getLanguage()));
        teamVO.setTeamDescription(fanTokenBTSHelper.getMessageByKey(teamVO.getTeamDescriptionKey(), baseHelper.getLanguage()));

        // team seo
        teamVO.setPageTitle(fanTokenBTSHelper.getMessageByKey(teamVO.getPageTitleKey(), baseHelper.getLanguage()));
        teamVO.setPageDesc(fanTokenBTSHelper.getMessageByKey(teamVO.getPageDescKey(), baseHelper.getLanguage()));
        teamVO.setOgTitle(fanTokenBTSHelper.getMessageByKey(teamVO.getOgTitleKey(), baseHelper.getLanguage()));
        teamVO.setOgDesc(fanTokenBTSHelper.getMessageByKey(teamVO.getOgDescKey(), baseHelper.getLanguage()));
    }

    public void doBannerI18n(BannerVO bannerVO) {

        bannerVO.setBannerTitle(fanTokenBTSHelper.getMessageByKey(bannerVO.getBannerTitleKey(), baseHelper.getLanguage()));
        bannerVO.setBannerButton(fanTokenBTSHelper.getMessageByKey(bannerVO.getBannerButtonKey(), baseHelper.getLanguage()));
        bannerVO.setBannerText(fanTokenBTSHelper.getMessageByKey(bannerVO.getBannerTextKey(), baseHelper.getLanguage()));
    }

    public void doUpdateEventI18n(List<VoteUpdateInfoVO> updateInfos) {

        updateInfos.forEach(u -> {
            u.setUpdateEventTitle(fanTokenBTSHelper.getMessageByKey(u.getUpdateEventTitleKey(), baseHelper.getLanguage()));
            u.setUpdateEventContent(fanTokenBTSHelper.getMessageByKey(u.getUpdateEventContentKey(),
                    baseHelper.getLanguage()));
        });
    }

    public void doVoteI18n(VoteVO voteVO) {

        String teamNameKey = voteVO.getTeamConfig().getTeamNameKey();
        String voteNameKey = voteVO.getVoteConfig().getVoteNameKey();
        String voteDescKey = voteVO.getVoteConfig().getVoteDescKey();
        // 设置team i18n
        voteVO.getTeamConfig().setTeamName(fanTokenBTSHelper.getMessageByKey(teamNameKey,baseHelper.getLanguage()));
        // 设置vote i18n
        voteVO.getVoteConfig().setVoteName(fanTokenBTSHelper.getMessageByKey(voteNameKey,baseHelper.getLanguage()));
        voteVO.getVoteConfig().setVoteDesc(fanTokenBTSHelper.getMessageByKey(voteDescKey,baseHelper.getLanguage()));
        // 设置option i18n
        voteVO.getVoteOptionConfigs().forEach(
                option ->{
                    option.setOptionDesc(fanTokenBTSHelper.getMessageByKey(option.getOptionDescKey(),baseHelper.getLanguage()));
                    option.setOptionName(fanTokenBTSHelper.getMessageByKey(option.getOptionNameKey(),baseHelper.getLanguage()));
                }
        );
    }

    /**
     * <h2>Category 展示信息国际化</h2>
     * */
    public void doCategoryI18n(List<CategoryDisplayVO> categoryDisplayVOS) {

        if (CollectionUtils.isNotEmpty(categoryDisplayVOS)) {
            categoryDisplayVOS.forEach(c -> {
                if (CollectionUtils.isNotEmpty(c.getCategorySets())) {
                        c.getCategorySets().forEach(cs -> {

                            String categorySetNameKey = cs.getCategorySetNameKey();
                            String categorySetDescriptionKey = cs.getCategorySetDescriptionKey();

                            cs.setCategorySetName(fanTokenBTSHelper.getMessageByKey(categorySetNameKey, baseHelper.getLanguage()));
                            cs.setCategorySetDescription(fanTokenBTSHelper.getMessageByKey(categorySetDescriptionKey, baseHelper.getLanguage()));
                        });
                }
            });
        }
    }

    /**
     * <h2>nft staking display 展示信息国际化</h2>
     * */
    public void doNftStakingDisplayI18n(NftStakingDisplayResponse nftStakingDisplayResponse) {

        if (null != nftStakingDisplayResponse
                && CollectionUtils.isNotEmpty(nftStakingDisplayResponse.getStakingInfos())) {
            nftStakingDisplayResponse.getStakingInfos().forEach(
                    d -> d.setStakingName(fanTokenBTSHelper.getMessageByKey(d.getStakingNameKey(), baseHelper.getLanguage()))
            );
        }
    }

    /**
     * <h2>领取质押收益历史信息国际化</h2>
     * */
    public void doStakingClaimHistory(List<DistributionHistoryResponse> responses) {

        if (CollectionUtils.isNotEmpty(responses)) {
            responses.forEach(r -> r.setStakingName(fanTokenBTSHelper.getMessageByKey(r.getStakingNameKey(), baseHelper.getLanguage())));
        }
    }

    /**
     * <h2>计算质押收益历史信息国际化</h2>
     * */
    public void doStakingCalculationHistory(List<CalculationHistoryResponse> responses) {

        if (CollectionUtils.isNotEmpty(responses)) {
            responses.forEach(r -> r.setStakingName(fanTokenBTSHelper.getMessageByKey(r.getStakingNameKey(), baseHelper.getLanguage())));
        }
    }

    /**
     * <h2>用户可用的 NFT Asset 信息国际化</h2>
     * */
    public void doStakingUsableNft(List<UsableNftResponse> responses) {

        if (CollectionUtils.isNotEmpty(responses)) {
            responses.forEach(r -> {
                r.setStakingName(fanTokenBTSHelper.getMessageByKey(r.getStakingNameKey(), baseHelper.getLanguage()));
                r.setSetName(fanTokenBTSHelper.getMessageByKey(r.getSetNameKey(), baseHelper.getLanguage()));
            });
        }
    }

    /**
     * <h2>用户可用的 NFT Asset 信息国际化</h2>
     * */
    public void doStakingStakingHistory(List<StakingHistoryResponse> responses) {

        if (CollectionUtils.isNotEmpty(responses)) {
            responses.forEach(r ->
                    r.setStakingName(fanTokenBTSHelper.getMessageByKey(r.getStakingNameKey(), baseHelper.getLanguage())));
        }
    }

    /**
     * <h2>商品信息国际化</h2>
     * */
    public void doFanShopItemInfo(FanShopItemResponse response) {

        if (null != response) {
            response.setItemName(fanTokenBTSHelper.getMessageByKey(response.getItemNameKey(), baseHelper.getLanguage()));
            response.setItemDescription(fanTokenBTSHelper.getMessageByKey(response.getItemDescriptionKey(), baseHelper.getLanguage()));
        }
    }

    /**
     * <h2>商品列表信息国际化</h2>
     * */
    public void doPageFanShopItemInfo(List<FanShopItemResponse> responses) {

        if (CollectionUtils.isNotEmpty(responses)) {
            responses.forEach(this::doFanShopItemInfo);
        }
    }

    /**
     * <h2>用户结算</h2>
     * */
    public void doItemCheckout(FanShopCheckoutResponse response) {

        if (null != response && CollectionUtils.isNotEmpty(response.getItems())) {
            response.getItems().forEach(this::doItemWithTeamInfo);
        }
    }

    /**
     * <h2>当个订单详情</h2>
     * */
    public void doOrderDetail(OrderDetailResponse response) {

        if (null != response && CollectionUtils.isNotEmpty(response.getItems())) {
            response.getItems().forEach(this::doItemWithTeamInfo);
        }
    }

    /**
     * <h2>订单历史</h2>
     * */
    public void doPageOrderDetail(List<OrderDetailResponse> responses) {

        if (CollectionUtils.isNotEmpty(responses)) {
            responses.forEach(this::doOrderDetail);
        }
    }

    /**
     * <h2>ItemWithTeamInfo i18n</h2>
     * */
    private void doItemWithTeamInfo(ItemWithTeamInfo info) {

        info.setTeamName(fanTokenBTSHelper.getMessageByKey(info.getTeamNameKey(), baseHelper.getLanguage()));
        if (null != info.getItemInfo()) {
            info.getItemInfo().setItemName(fanTokenBTSHelper.getMessageByKey(info.getItemInfo().getItemNameKey(),
                    baseHelper.getLanguage()));
            info.getItemInfo().setItemDescription(fanTokenBTSHelper.getMessageByKey(
                    info.getItemInfo().getItemDescriptionKey(), baseHelper.getLanguage()));
        }
    }

    /**
     * <h2>填充 AcceptOrderRequest 中用户的下单 i18n</h2>
     * 需要注意: 目前只支持单个商品的下单操作
     * */
    public void doAcceptOrderRequest(AcceptOrderRequest request, SimpleItemInfo itemInfo) {

        if (null != request && CollectionUtils.isNotEmpty(request.getItemInfos()) && null != itemInfo) {
            request.getItemInfos().forEach(i -> {

                i.setItemName(fanTokenBTSHelper.getMessageByKey(itemInfo.getItemNameKey(), baseHelper.getLanguage()));
                i.setItemNameKey(itemInfo.getItemNameKey());
            });
        }
    }

    /**
     * <h2>填充 SimpleTeamInfo 中的 i18n</h2>
     * */
    public void doSimpleTeamInfo(SimpleTeamInfo teamInfo) {

        if (null != teamInfo) {
            teamInfo.setTeamName(fanTokenBTSHelper.getMessageByKey(teamInfo.getTeamNameKey(), baseHelper.getLanguage()));
        }
    }

    /**
     * <h2>填充 SimpleTeamInfo 中的 i18n</h2>
     * */
    public void doSimpleTeamInfoList(List<SimpleTeamInfo> teamInfos) {

        if (CollectionUtils.isNotEmpty(teamInfos)) {
            teamInfos.forEach(this::doSimpleTeamInfo);
        }
    }

    /**
     * <h2>填充 VotingPollsVO 中的 i18n</h2>
     * */
    public void doVotingPollsInfo(VotingPollsVO votingPollsVO) {

        if (null != votingPollsVO) {
            doSimpleTeamInfo(votingPollsVO.getTeamInfo());
            doMvpVoteConfigInfo(votingPollsVO.getMvpVoteInfo());
            doLeaderboardList(votingPollsVO.getLeaderBoard());
            doPlayerVoteInfoList(votingPollsVO.getPlayerVotes());
        }
    }

    /**
     * <h2>填充 VotingPollsVO 中的 i18n</h2>
     * */
    public void doVotingPollsInfoList(List<VotingPollsVO> votingPollsVOs) {

        if (CollectionUtils.isNotEmpty(votingPollsVOs)) {
            votingPollsVOs.forEach(this::doVotingPollsInfo);
        }
    }

    /**
     * <h2>填充 MvpVoteConfigVO 中的 i18n</h2>
     * */
    public void doMvpVoteConfigInfo(MvpVoteConfigVO configVO) {

        if (null != configVO) {
            configVO.setMvpVotePageName(fanTokenBTSHelper.getMessageByKey(configVO.getMvpVotePageNameKey(), baseHelper.getLanguage()));
            configVO.setMvpVoteEventName(fanTokenBTSHelper.getMessageByKey(configVO.getMvpVoteEventNameKey(), baseHelper.getLanguage()));
            configVO.setMvpVoteDesc(fanTokenBTSHelper.getMessageByKey(configVO.getMvpVoteDescKey(), baseHelper.getLanguage()));
        }
    }

    /**
     * <h2>填充排行榜中的 i18n</h2>
     * */
    public void doLeaderboard(LeaderBoard leaderBoard) {

        if (null != leaderBoard) {
            leaderBoard.setPlayerName(fanTokenBTSHelper.getMessageByKey(leaderBoard.getPlayerNameKey(), baseHelper.getLanguage()));
        }
    }

    /**
     * <h2>填充排行榜中的 i18n</h2>
     * */
    public void doLeaderboardList(List<LeaderBoard> leaderBoards) {

        if (CollectionUtils.isNotEmpty(leaderBoards)) {
            leaderBoards.forEach(this::doLeaderboard);
        }
    }

    /**
     * <h2>填充 player vote 中的 i18n</h2>
     * */
    public void doPlayerVoteInfo(PlayerVoteInfo voteInfo) {

        if (null != voteInfo) {
            voteInfo.setVoteName(fanTokenBTSHelper.getMessageByKey(voteInfo.getVoteNameKey(), baseHelper.getLanguage()));
            if (null != voteInfo.getVoteDesc()) {
                voteInfo.setVoteDesc(fanTokenBTSHelper.getMessageByKey(voteInfo.getVoteDescKey(), baseHelper.getLanguage()));
            }
        }
    }

    /**
     * <h2>填充 player vote 中的 i18n</h2>
     * */
    public void doPlayerVoteInfoList(List<PlayerVoteInfo> voteInfos) {

        if (CollectionUtils.isNotEmpty(voteInfos)) {
            voteInfos.forEach(this::doPlayerVoteInfo);
        }
    }

    /**
     * <h2>填充 mvp vote update 中的 i18n</h2>
     * */
    public void doMvpVoteUpdateInfo(MvpVoteUpdateVO updateVO) {

        if (null != updateVO) {
            updateVO.setUpdateEventTitle(fanTokenBTSHelper.getMessageByKey(updateVO.getUpdateEventTitleKey(), baseHelper.getLanguage()));
            updateVO.setUpdateEventContent(fanTokenBTSHelper.getMessageByKey(updateVO.getUpdateEventContentKey(), baseHelper.getLanguage()));
        }
    }

    /**
     * <h2>填充 mvp vote update 中的 i18n</h2>
     * */
    public void doMvpVoteUpdateInfoList(List<MvpVoteUpdateVO> updateVOs) {

        if (CollectionUtils.isNotEmpty(updateVOs)) {
            updateVOs.forEach(this::doMvpVoteUpdateInfo);
        }
    }

    /**
     * <h2>填充 team player 中的 i18n</h2>
     * */
    public void doTeamPlayerInfo(TeamPlayerVO playerVO) {

        if (null != playerVO) {
            playerVO.setPlayerName(fanTokenBTSHelper.getMessageByKey(playerVO.getPlayerNameKey(), baseHelper.getLanguage()));
            playerVO.setPlayerDesc(fanTokenBTSHelper.getMessageByKey(playerVO.getPlayerDescKey(), baseHelper.getLanguage()));
        }
    }

    /**
     * <h2>填充 team player 中的 i18n</h2>
     * */
    public void doTeamPlayerInfoList(List<TeamPlayerVO> playerVOs) {

        if (CollectionUtils.isNotEmpty(playerVOs)) {
            playerVOs.forEach(this::doTeamPlayerInfo);
        }
    }

    /**
     * <h2>填充 MvpVotingDetailVO 中的 i18n</h2>
     * */
    public void doMvpVotingDetailVO(MvpVotingDetailVO detailVO) {

        if (null != detailVO) {
            doMvpVoteConfigInfo(detailVO.getMvpVoteInfo());
            doLeaderboardList(detailVO.getLeaderBoard());
            doPlayerVoteInfoList(detailVO.getPlayerVotes());
            doMvpVoteUpdateInfoList(detailVO.getVoteUpdates());
        }
    }

    /**
     * <h2>填充 PlayerVoteDetailVO 中的 i18n</h2>
     * */
    public void doPlayerVoteDetailVO(PlayerVoteDetailVO detailVO) {

        if (null != detailVO) {

            doSimpleTeamInfo(detailVO.getTeamInfo());
            doMvpVoteConfigInfo(detailVO.getMvpVoteInfo());
            doPlayerVoteInfo(detailVO.getVoteInfo());

            if (null != detailVO.getFormationArea()) {
                doTeamPlayerInfoList(detailVO.getFormationArea().getForward());
                doTeamPlayerInfoList(detailVO.getFormationArea().getMiddlefielder());
                doTeamPlayerInfoList(detailVO.getFormationArea().getBackfielder());
                doTeamPlayerInfoList(detailVO.getFormationArea().getGoalkeeper());
                doTeamPlayerInfoList(detailVO.getFormationArea().getSubstitute());
            }
        }
    }

    /**
     * <h2>填充 PlayerVoteOptionDetailVO 中的 i18n</h2>
     * */
    public void doPlayerVoteOptionDetailVO(PlayerVoteOptionDetailVO optionDetailVO) {

        if (null != optionDetailVO) {
            doPlayerVoteInfo(optionDetailVO.getVoteInfo());
            doTeamPlayerInfoList(optionDetailVO.getPlayerOptions());
        }
    }

    /////////////////////////////////////////////// Prediction ///////////////////////////////////////////////

    /**
     * <h2>填充简单的 Campaign 信息</h2>
     * */
    public void doPredictionCampaignSimpleInfo(List<PredictionCampaignEventInfo.PredictionCampaignSimpleInfo>
                                                       campaignSimpleInfos) {

        if (CollectionUtils.isNotEmpty(campaignSimpleInfos)) {
            campaignSimpleInfos.forEach(c -> {
                if (StringUtils.isNotBlank(c.getCampaignNameKey())) {
                    c.setCampaignName(fanTokenBTSHelper.getMessageByKey(c.getCampaignNameKey(), baseHelper.getLanguage()));
                }
            });
        }
    }

    /**
     * <h2>填充 Campaign 的基本信息</h2>
     * */
    public void doPredictionCampaignBaseInfo(PredictionCampaignBaseInfo campaignBaseInfo) {

        if (null != campaignBaseInfo) {
            campaignBaseInfo.setCampaignName(fanTokenBTSHelper.getMessageByKey(campaignBaseInfo.getCampaignNameKey(),
                    baseHelper.getLanguage()));
        }
    }

    /**
     * <h2>填充 PredictionTeam 信息</h2>
     * */
    public void doPredictionTeamVO(PredictionTeamVO predictionTeamVO) {

        if (null != predictionTeamVO) {
            predictionTeamVO.setPredictionTeamName(fanTokenBTSHelper.getMessageByKey(
                    predictionTeamVO.getPredictionTeamNameKey(), baseHelper.getLanguage()));
        }
    }

    /**
     * <h2>填充 PredictionInfo 信息</h2>
     * */
    public void doPredictionInfo(PredictionInfo predictionInfo) {

        if (null != predictionInfo) {
            doPredictionTeamVO(predictionInfo.getTeamA());
            doPredictionTeamVO(predictionInfo.getTeamB());
        }
    }

    /**
     * <h2>填充 PredictionCampaignUserInfo</h2>
     * */
    public void doPredictionCampaignUserInfo(PredictionCampaignUserInfo userInfo) {

        if (null != userInfo && null != userInfo.getUserRewardsInfo()
                && null != userInfo.getUserRewardsInfo().getRewardsInfo()) {
            PredictionUserRewardsInfo.PredictionRewardsInfo rewardsInfo = userInfo.getUserRewardsInfo().getRewardsInfo();
            if (StringUtils.isNotBlank(rewardsInfo.getRewardsTitleKey())) {
                rewardsInfo.setRewardsTitle(fanTokenBTSHelper.getMessageByKey(rewardsInfo.getRewardsTitleKey(), baseHelper.getLanguage()));
            }
            if (StringUtils.isNotBlank(rewardsInfo.getRewardsDescKey())) {
                rewardsInfo.setRewardsDesc(fanTokenBTSHelper.getMessageByKey(rewardsInfo.getRewardsDescKey(), baseHelper.getLanguage()));
            }
        }
    }

    /**
     * <h2>填充 PredictionCampaignInfo 信息</h2>
     * */
    public void doPredictionCampaignInfo(PredictionCampaignInfo predictionCampaignInfo) {

        if (null != predictionCampaignInfo) {
            doPredictionCampaignUserInfo(predictionCampaignInfo.getUserInfo());
            doPredictionCampaignBaseInfo(predictionCampaignInfo.getCampaignInfo());
            if (CollectionUtils.isNotEmpty(predictionCampaignInfo.getPredictionInfos())) {
                predictionCampaignInfo.getPredictionInfos().forEach(this::doPredictionInfo);
            }
        }
    }

    /**
     * <h2>填充 PredictionCampaignLeaderboardInfo 信息</h2>
     * */
    public void doPredictionCampaignLeaderboardInfo(PredictionCampaignLeaderboardInfo leaderboardInfo) {

        if (null != leaderboardInfo && null != leaderboardInfo.getCampaignInfo()) {
            doSimpleTeamInfo(leaderboardInfo.getTeamInfo());
            doPredictionCampaignBaseInfo(leaderboardInfo.getCampaignInfo());
        }
    }

    /**
     * <h2>填充 PredictionRewardsVO 信息</h2>
     * */
    public void doPredictionRewardsVO(PredictionRewardsVO rewardsVO) {

        if (null != rewardsVO) {
            rewardsVO.setRewardsTitle(fanTokenBTSHelper.getMessageByKey(rewardsVO.getRewardsTitleKey(), baseHelper.getLanguage()));
            if (StringUtils.isNotBlank(rewardsVO.getRewardsDescKey())) {
                rewardsVO.setRewardsDesc(fanTokenBTSHelper.getMessageByKey(rewardsVO.getRewardsDescKey(), baseHelper.getLanguage()));
            }
        }
    }

    /**
     * <h2>填充 PredictionCampaignRewardsInfo 信息</h2>
     * */
    public void doPredictionCampaignRewardsInfo(PredictionCampaignRewardsInfo rewardsInfo) {

        if (null != rewardsInfo) {
            doSimpleTeamInfo(rewardsInfo.getTeamInfo());
            doPredictionCampaignBaseInfo(rewardsInfo.getCampaignInfo());
            if (null != rewardsInfo.getRewards() && CollectionUtils.isNotEmpty(rewardsInfo.getRewards().getData())) {
                rewardsInfo.getRewards().getData().forEach(this::doPredictionRewardsVO);
            }
        }
    }

    /**
     * <h2>填充 PredictionBaseInfo 信息</h2>
     * */
    public void doPredictionBaseInfo(PredictionBaseInfo predictionBaseInfo) {

        if (null != predictionBaseInfo) {
            doPredictionTeamVO(predictionBaseInfo.getTeamA());
            doPredictionTeamVO(predictionBaseInfo.getTeamB());
            predictionBaseInfo.setMatchDesc(fanTokenBTSHelper.getMessageByKey(predictionBaseInfo.getMatchDescKey(), baseHelper.getLanguage()));
        }
    }

    /**
     * <h2>填充 PredictionProfileInfo 信息</h2>
     * */
    public void doPredictionProfileInfo(PredictionProfileInfo profileInfo) {

        if (null != profileInfo) {
            doSimpleTeamInfo(profileInfo.getTeamInfo());
            doPredictionBaseInfo(profileInfo.getPredictionInfo());
        }
    }

    /**
     * <h2>填充 InviterPosterVO 信息</h2>
     * */
    public void doInviterPosterVO(InviterPosterVO posterVO) {

        if (null != posterVO) {
            doSimpleTeamInfo(posterVO.getTeamInfo());
            doPredictionInfo(posterVO.getPredictionInfo());
        }
    }

    /**
     * <h2>填充 FanCollection 信息</h2>
     * */
    public void doDisplayFanCollection(List<FanCollectionVO> fanCollectionVOS) {

        if (CollectionUtils.isNotEmpty(fanCollectionVOS)) {
            fanCollectionVOS.forEach(this::doFanCollectionVO);
        }
    }

    /**
     * <h2>填充 FanCollectionVO 信息</h2>
     * */
    public void doFanCollectionVO(FanCollectionVO fanCollectionVO) {

        if (null != fanCollectionVO) {
            if (null != fanCollectionVO.getCategorySet()) {
                CategoryDisplayVO.CategorySetsDTO categorySet = fanCollectionVO.getCategorySet();
                categorySet.setCategorySetName(fanTokenBTSHelper.getMessageByKey(categorySet.getCategorySetNameKey(), baseHelper.getLanguage()));
                categorySet.setCategorySetDescriptionKey(fanTokenBTSHelper.getMessageByKey(categorySet.getCategorySetDescriptionKey(), baseHelper.getLanguage()));
            }
        }
    }

    /**
     * <h2> 填充 FanProfile Tag Name </h2>
     */
    public void doFanprofileTagInfos(List<TagInfoVO> tagInfos) {
        if (CollectionUtils.isNotEmpty(tagInfos)) {
            tagInfos.forEach(this::doFanprofileTagInfo);
        }
    }

    /**
     * <h2> 填充 FanProfile Tag Name </h2>
     */
    public void doFanprofileTagInfo(TagInfoVO tagInfoVO) {
        if (null != tagInfoVO) {
            tagInfoVO.setTagName(fanTokenBTSHelper.getMessageByKey(tagInfoVO.getTagNameKey(), baseHelper.getLanguage()));
        }
    }

    /**
     * <h2>填充 MainVenueVO</h2>
     * */
    public void doMainVenueVO(MainVenueVO mainVenueVO) {

        if (null != mainVenueVO) {
            doSimpleTeamInfo(mainVenueVO.getTeamInfo());
            if (null != mainVenueVO.getCampaignInfo()) {
                mainVenueVO.getCampaignInfo().setCampaignName(fanTokenBTSHelper.getMessageByKey(mainVenueVO.getCampaignInfo().getCampaignNameKey(), baseHelper.getLanguage()));
                mainVenueVO.getCampaignInfo().setCampaignDescription(fanTokenBTSHelper.getMessageByKey(mainVenueVO.getCampaignInfo().getCampaignDescriptionKey(), baseHelper.getLanguage()));
                if (CollectionUtils.isNotEmpty(mainVenueVO.getCampaignInfo().getCampaignPieceInfos())) {
                    mainVenueVO.getCampaignInfo().getCampaignPieceInfos().forEach(this::doCampaignPieceInfo);
                }
            }
        }
    }

    /**
     * <h2>填充 BranchVenueVO</h2>
     * */
    public void doBranchVenueVO(BranchVenueVO branchVenueVO) {

        if (null != branchVenueVO) {
            doSimpleTeamInfo(branchVenueVO.getTeamInfo());
            if (CollectionUtils.isNotEmpty(branchVenueVO.getCampaignPieceInfos())) {
                branchVenueVO.getCampaignPieceInfos().forEach(this::doCampaignPieceInfo);
            }
        }
    }

    /**
     * <h2>填充 CampaignPieceInfo</h2>
     * */
    public void doCampaignPieceInfo(CampaignPieceInfo p) {

        if (null != p) {
            p.setPieceName(fanTokenBTSHelper.getMessageByKey(p.getPieceNameKey(), baseHelper.getLanguage()));
            p.setPieceDescription(fanTokenBTSHelper.getMessageByKey(p.getPieceDescriptionKey(), baseHelper.getLanguage()));
        }
    }

    public void doTicketNftBenefitInfo(TicketNftBenefitInfo benefitInfo) {

        if (null != benefitInfo) {
            benefitInfo.setBenefitDescription(fanTokenBTSHelper.getMessageByKey(benefitInfo.getBenefitDescriptionKey(),
                    baseHelper.getLanguage()));
        }
    }

    public void doTicketNftEventInfo(TicketNftEventInfo e) {

        if (null != e) {
            e.setEventName(fanTokenBTSHelper.getMessageByKey(e.getEventNameKey(), baseHelper.getLanguage()));
            e.setEventDescription(fanTokenBTSHelper.getMessageByKey(e.getEventDescriptionKey(), baseHelper.getLanguage()));
            if (CollectionUtils.isNotEmpty(e.getBenefitInfos())) {
                e.getBenefitInfos().forEach(this::doTicketNftBenefitInfo);
            }
        }
    }

    public void doSingleMatchInfo(SingleMatchInfo m) {

        if (null != m) {
            m.setMatchCompetitor(fanTokenBTSHelper.getMessageByKey(m.getMatchCompetitorKey(), baseHelper.getLanguage()));
            if (CollectionUtils.isNotEmpty(m.getRewardInfos())) {
                m.getRewardInfos().forEach(r -> r.setRewardName(fanTokenBTSHelper.getMessageByKey(r.getRewardNameKey(),
                        baseHelper.getLanguage())));
            }
        }
    }

    public void doTicketNftCardDetailVO(TicketNftCardDetailVO cardDetailVO) {

        if (null != cardDetailVO) {
            doSimpleTeamInfo(cardDetailVO.getTeamInfo());
            doTicketNftEventInfo(cardDetailVO.getEventInfo());
            doSingleMatchInfo(cardDetailVO.getSingleMatchInfo());
        }
    }

    public void doTicketNftEventCardDetailPageResponse(TicketNftEventCardDetailPageResponse response) {

        if (null != response) {
            doSimpleTeamInfo(response.getTeamInfo());
            if (CollectionUtils.isNotEmpty(response.getData())) {
                response.getData().forEach(this::doTicketNftEventInfo);
            }
        }
    }

    public void doSingleTicketCardDetailPageResponse(SingleTicketCardDetailPageResponse response) {

        if (null != response) {
            doSimpleTeamInfo(response.getTeamInfo());
            if (CollectionUtils.isNotEmpty(response.getData())) {
                response.getData().forEach(this::doTicketNftCardDetailVO);
            }
        }
    }

    public void doTicketNftCardInformationVO(TicketNftCardInformationVO cardInformationVO) {

        if (null != cardInformationVO) {
            doSingleMatchInfo(cardInformationVO.getSingleMatchInfo());
        }
    }

    public void doSingleTicketCardRewardVO(SingleTicketCardRewardVO cr) {

        if (null != cr && CollectionUtils.isNotEmpty(cr.getRewardInfos())) {
            cr.getRewardInfos().forEach(r -> {
                r.setRewardName(fanTokenBTSHelper.getMessageByKey(r.getRewardNameKey(), baseHelper.getLanguage()));
                r.setRewardDescription(fanTokenBTSHelper.getMessageByKey(r.getRewardDescriptionKey(), baseHelper.getLanguage()));
            });
        }
    }

    public void doTicketNftCardBenefitVO(TicketNftCardBenefitVO cardBenefitVO) {

        if (null != cardBenefitVO) {
            if (CollectionUtils.isNotEmpty(cardBenefitVO.getBenefitInfos())) {
                cardBenefitVO.getBenefitInfos().forEach(this::doTicketNftBenefitInfo);
            }
        }
    }

    /**
     * <h2>用户结算</h2>
     * */
    public void doFiatItemCheckout(FanShopFiatCheckoutResponse response) {
        if (null != response && CollectionUtils.isNotEmpty(response.getItems())) {
            response.getItems().forEach(this::doItemWithTeamInfo);
        }
    }
}
