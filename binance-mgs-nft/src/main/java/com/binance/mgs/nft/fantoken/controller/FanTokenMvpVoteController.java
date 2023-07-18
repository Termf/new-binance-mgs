package com.binance.mgs.nft.fantoken.controller;

import com.binance.master.models.APIRequest;
import com.binance.master.models.APIResponse;
import com.binance.platform.mgs.base.helper.BaseHelper;
import com.binance.platform.mgs.base.vo.CommonRet;
import com.binance.mgs.nft.fantoken.helper.FanTokenCheckHelper;
import com.binance.mgs.nft.mysterybox.helper.FanTokenI18nHelper;
import com.binance.nft.fantoken.ifae.IFanTokenMvpVoteManageApi;
import com.binance.nft.fantoken.request.CommonPageRequest;
import com.binance.nft.fantoken.request.CommonQueryRequest;
import com.binance.nft.fantoken.request.mvpvote.MvpVotingRequest;
import com.binance.nft.fantoken.response.CommonPageResponse;
import com.binance.nft.fantoken.response.VoidResponse;
import com.binance.nft.fantoken.vo.SimpleTeamInfo;
import com.binance.nft.fantoken.vo.mvpvote.LeaderBoard;
import com.binance.nft.fantoken.vo.mvpvote.MvpVotingDetailVO;
import com.binance.nft.fantoken.vo.mvpvote.PlayerVoteDetailVO;
import com.binance.nft.fantoken.vo.mvpvote.PlayerVoteOptionDetailVO;
import com.binance.nft.fantoken.vo.mvpvote.VotingPollsVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * <h1>Mvp Voting Controller</h1>
 * */
@Slf4j
@RequestMapping("/v1/friendly/nft/fantoken/mvpvoting")
@RestController
@RequiredArgsConstructor
public class FanTokenMvpVoteController {

    private final BaseHelper baseHelper;
    private final FanTokenI18nHelper fanTokenI18nHelper;
    private final FanTokenCheckHelper fanTokenCheckHelper;
    private final IFanTokenMvpVoteManageApi fanTokenMvpVoteManageApi;

    /**
     * <h2>球队信息</h2>
     * */
    @GetMapping("/fantoken-team")
    public CommonRet<List<SimpleTeamInfo>> fantokenTeam() {

        boolean isGray = fanTokenCheckHelper.isGray();
        Long userId = baseHelper.getUserId();

        CommonQueryRequest request = CommonQueryRequest.builder()
                .isGray(isGray)
                .userId(userId)
                .clientType(baseHelper.getClientType())
                .build();

        APIResponse<List<SimpleTeamInfo>> response = fanTokenMvpVoteManageApi.fantokenTeam(APIRequest.instance(request));
        baseHelper.checkResponse(response);
        List<SimpleTeamInfo> result = fanTokenCheckHelper.getGccComplianceTeamInfo(isGray, userId, response.getData());

        // i18n
        fanTokenI18nHelper.doSimpleTeamInfoList(result);

        return new CommonRet<>(result);
    }

    /**
     * <h2>球队 MVP Voting 展示页</h2>
     * */
    @PostMapping("/team-voting-polls")
    public CommonRet<VotingPollsVO> teamVotingPolls(@RequestBody CommonQueryRequest request) {

        // 用户可以不登录, 所以, 不需要校验 userId 是否是 null
        request.setIsGray(fanTokenCheckHelper.isGray());
        request.setUserId(baseHelper.getUserId());
        request.setClientType(baseHelper.getClientType());

        APIResponse<VotingPollsVO> response = fanTokenMvpVoteManageApi.teamVotingPolls(APIRequest.instance(request));
        baseHelper.checkResponse(response);

        // i18n
        VotingPollsVO result = response.getData();
        fanTokenI18nHelper.doVotingPollsInfo(result);

        // 需要对 leaderBoard 进行排序: 先按照 voteCount 倒序排, 再按照 playerName 正序排
        if (null != result && CollectionUtils.isNotEmpty(result.getLeaderBoard())) {
            result.setLeaderBoard(processLeaderBoardOrder(result.getLeaderBoard()));
        }

        return new CommonRet<>(result);
    }

    /**
     * <h2>最近的 MVP Voting 展示页</h2>
     * */
    @PostMapping("/latest-voting-polls")
    public CommonRet<CommonPageResponse<VotingPollsVO>> latestVotingPolls(@RequestBody CommonPageRequest<
            CommonQueryRequest> request) {

        // 用户可以不登录, 所以, 不需要校验 userId 是否是 null
        if (null != request.getParams()) {
            request.getParams().setIsGray(fanTokenCheckHelper.isGray());
            request.getParams().setUserId(baseHelper.getUserId());
            request.getParams().setClientType(baseHelper.getClientType());
        }

        APIResponse<CommonPageResponse<VotingPollsVO>> response = fanTokenMvpVoteManageApi.latestVotingPolls(
                APIRequest.instance(request));
        baseHelper.checkResponse(response);

        // i18n
        CommonPageResponse<VotingPollsVO> result = response.getData();
        fanTokenI18nHelper.doVotingPollsInfoList(result.getData());

        // 需要对 leaderBoard 进行排序: 先按照 voteCount 倒序排, 再按照 playerName 正序排
        if (CollectionUtils.isNotEmpty(result.getData())) {
            result.getData().forEach(d -> d.setLeaderBoard(processLeaderBoardOrder(d.getLeaderBoard())));
        }

        return new CommonRet<>(result);
    }

    /**
     * <h2>MVP Voting 详情页</h2>
     * */
    @PostMapping("/mvp-voting-detail")
    public CommonRet<MvpVotingDetailVO> mvpVotingDetail(@RequestBody MvpVotingRequest request) {

        // 用户可以不登录, 所以, 不需要校验 userId 是否是 null
        request.setUserId(baseHelper.getUserId());
        request.setClientType(baseHelper.getClientType());
        request.setIsGray(fanTokenCheckHelper.isGray());

        APIResponse<MvpVotingDetailVO> response = fanTokenMvpVoteManageApi.mvpVotingDetail(APIRequest.instance(request));
        baseHelper.checkResponse(response);

        // i18n
        MvpVotingDetailVO result = response.getData();
        fanTokenI18nHelper.doMvpVotingDetailVO(result);

        // 需要对 leaderBoard 进行排序: 先按照 voteCount 倒序排, 再按照 playerName 正序排
        if (null != result && CollectionUtils.isNotEmpty(result.getLeaderBoard())) {
            result.setLeaderBoard(processLeaderBoardOrder(result.getLeaderBoard()));
        }

        return new CommonRet<>(result);
    }

    /**
     * <h2>处理排行榜的顺序</h2>
     * */
    private List<LeaderBoard> processLeaderBoardOrder(List<LeaderBoard> leaderBoard) {

        if (CollectionUtils.isNotEmpty(leaderBoard)) {
            List<LeaderBoard> leaderBoards = leaderBoard.stream()
                    .sorted(Comparator.comparing(LeaderBoard::getVoteCount).reversed().thenComparing(LeaderBoard::getPlayerName))
                    .collect(Collectors.toList());
            int ranking = 1;
            for (LeaderBoard itemBoard : leaderBoards) {
                itemBoard.setRanking(ranking);
                ranking += 1;
            }
            return leaderBoards;
        }

        return leaderBoard;
    }

    /**
     * <h2>Player Vote 详情页</h2>
     * */
    @PostMapping("/player-vote-detail")
    public CommonRet<PlayerVoteDetailVO> playerVoteDetail(@RequestBody MvpVotingRequest request) {

        // 用户可以不登录, 所以, 不需要校验 userId 是否是 null
        request.setUserId(baseHelper.getUserId());
        request.setClientType(baseHelper.getClientType());
        request.setIsGray(fanTokenCheckHelper.isGray());

        APIResponse<PlayerVoteDetailVO> response = fanTokenMvpVoteManageApi.playerVoteDetail(APIRequest.instance(request));
        baseHelper.checkResponse(response);

        // i18n
        fanTokenI18nHelper.doPlayerVoteDetailVO(response.getData());

        return new CommonRet<>(response.getData());
    }

    /**
     * <h2>用户投票</h2>
     * 1. 需要用户登录
     * 2. 需要强制 KYC
     * */
    @PostMapping("/user-vote-player")
    public CommonRet<VoidResponse> userVotePlayer(@RequestBody MvpVotingRequest request) {

        Long userId = baseHelper.getUserId();
        if (null == userId) {
            return new CommonRet<>(new VoidResponse());
        }

        // 强制 KYC 的校验
        fanTokenCheckHelper.userComplianceValidate(userId);

        request.setUserId(userId);
        request.setClientType(baseHelper.getClientType());
        request.setComplianceAssetDto(fanTokenCheckHelper.fanTokenComplianceAsset(baseHelper.getUserId()));

        APIResponse<VoidResponse> response = fanTokenMvpVoteManageApi.userVotePlayer(APIRequest.instance(request));
        baseHelper.checkResponse(response);
        return new CommonRet<>(response.getData());
    }

    /**
     * <h2>Player Vote 选项详情页</h2>
     * */
    @PostMapping("/player-vote-option-detail")
    public CommonRet<PlayerVoteOptionDetailVO> playerVoteOptionDetail(@RequestBody MvpVotingRequest request) {

        // 用户可以不登录, 所以, 不需要校验 userId 是否是 null
        request.setUserId(baseHelper.getUserId());
        request.setClientType(baseHelper.getClientType());

        APIResponse<PlayerVoteOptionDetailVO> response = fanTokenMvpVoteManageApi.playerVoteOptionDetail(
                APIRequest.instance(request));
        baseHelper.checkResponse(response);

        // i18n
        fanTokenI18nHelper.doPlayerVoteOptionDetailVO(response.getData());

        return new CommonRet<>(response.getData());
    }
}
