package com.binance.mgs.nft.fantoken.controller;

import com.alibaba.fastjson.JSON;
import com.binance.master.models.APIRequest;
import com.binance.master.models.APIResponse;
import com.binance.master.utils.IPUtils;
import com.binance.master.utils.WebUtils;
import com.binance.nft.fantoken.vo.SimpleTeamInfo;
import com.binance.platform.mgs.base.helper.BaseHelper;
import com.binance.platform.mgs.base.vo.CommonRet;
import com.binance.mgs.nft.fantoken.helper.CacheProperty;
import com.binance.mgs.nft.fantoken.helper.FanTokenCacheHelper;
import com.binance.mgs.nft.fantoken.helper.FanTokenCheckHelper;
import com.binance.mgs.nft.mysterybox.helper.FanTokenI18nHelper;
import com.binance.nft.fantoken.constant.CallerTypeEnum;
import com.binance.nft.fantoken.constant.VoteStatusEnum;
import com.binance.nft.fantoken.ifae.IFanTokenVoteManageAPI;
import com.binance.nft.fantoken.request.*;
import com.binance.nft.fantoken.response.*;
import com.binance.nft.fantoken.vo.OptionStatisticVO;
import com.binance.nft.fantoken.vo.VoteVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * <h1>fantoken vote</h1>
 * 2022.09.09 vote 取消 kyc
 * */
@Slf4j
@RequestMapping("/v1/friendly/nft/fantoken")
@RestController
@RequiredArgsConstructor
public class FanTokenVoteController {

    private final CacheProperty cacheProperty;

    private final IFanTokenVoteManageAPI fanTokenVoteManageAPI;
    private final BaseHelper baseHelper;
    private final FanTokenCacheHelper fanTokenCacheHelper;
    private final FanTokenI18nHelper fanTokenI18nHelper;
    private final FanTokenCheckHelper fanTokenCheckHelper;

    @PostMapping("/query-vote-by-page")
    public CommonRet<CommonPageResponse<VoteVO>> queryVoteByPage(
            @Valid @RequestBody CommonPageRequest<QueryVoteByPageRequest> request) throws Exception {

        boolean isGrey = fanTokenCheckHelper.isGray();

        // use cache
        if (false && !isGrey && cacheProperty.isEnabled()) {
            return queryPageVoteByCache(request);
        } else {
            request.getParams().setCallerType(CallerTypeEnum.CUSTOM.getCallerType());
            request.getParams().setIsGray(isGrey);

            APIResponse<CommonPageResponse<VoteVO>> response = fanTokenVoteManageAPI.queryVoteByPage(APIRequest.instance(request));
            baseHelper.checkResponse(response);
            response.getData().getData().forEach(fanTokenI18nHelper::doVoteI18n);
            recalculateVoteStatus(response.getData().getData());

            return new CommonRet<>(response.getData());
        }
    }

    /**
     * <h2>query page vote from guava cache</h2>
     * */
    private CommonRet<CommonPageResponse<VoteVO>> queryPageVoteByCache(
            CommonPageRequest<QueryVoteByPageRequest> request) throws Exception {

        log.info("query page vote use cache: [{}]", JSON.toJSONString(request));

        CommonPageResponse<VoteVO> allVoteVos = fanTokenCacheHelper.queryVote();
        if (null == allVoteVos || CollectionUtils.isEmpty(allVoteVos.getData())) {
            return new CommonRet<>(
                    CommonPageResponse.<VoteVO>builder().page(request.getPage()).size(request.getSize())
                            .total(0).data(Collections.emptyList()).build());
        }

        // ii18n
        allVoteVos.getData().forEach(fanTokenI18nHelper::doVoteI18n);
        // recalculate vote status
        recalculateVoteStatus(allVoteVos.getData());

        // 按照 teamId 过滤, 再按照 order by vote_rank asc, 最后翻转
        // params 中不一定存在 query(team_id)
        List<VoteVO> filterVoteVOs;
        if (null == request.getParams() || StringUtils.isBlank(request.getParams().getQuery())) {
            filterVoteVOs = allVoteVos.getData().stream()
                    .sorted(Comparator.comparing(o -> o.getVoteConfig().getVoteRank()))
                    .collect(Collectors.toList());
        } else {
            Map<String, String> queryMap = (Map) JSON.parseObject(request.getParams().getQuery());
            filterVoteVOs = allVoteVos.getData().stream()
                    .filter(v -> v.getTeamConfig().getTeamId().equals(queryMap.get("team_id")))
                    .sorted(Comparator.comparing(o -> o.getVoteConfig().getVoteRank()))
                    .collect(Collectors.toList());
        }

        Collections.reverse(filterVoteVOs);

        // 分页
        int page = Objects.isNull(request.getPage()) || request.getPage() <= 0 ? 1 : request.getPage();
        int size = Objects.isNull(request.getSize()) || request.getSize() <= 0 ? 10 : request.getSize();
        if ((page - 1) * size <= filterVoteVOs.size()) {
            List<VoteVO> result = filterVoteVOs.subList(
                    (page - 1) * size, Math.min(page * size, filterVoteVOs.size())
            );
            return new CommonRet<>(
                    CommonPageResponse.<VoteVO>builder().page(request.getPage()).size(request.getSize())
                            .total(filterVoteVOs.size()).data(result).build()
            );
        }

        return new CommonRet<>(
                CommonPageResponse.<VoteVO>builder().page(request.getPage()).size(request.getSize())
                        .total(filterVoteVOs.size()).data(Collections.emptyList()).build()
        );
    }

    /**
     * <h2>根据 voteId 去查询 vote 的详细信息</h2>
     * 不需要考虑灰度环境的问题, 因为之后 vote page 接口暴露出来 voteId 才可能会调用这个接口, 在 vote page 中过滤灰度的情况即可
     * */
    @GetMapping("/query-vote-info")
    public CommonRet<VoteVO> queryVoteInfo(String voteId) throws Exception {
        // use cache
        if (false) {
            log.info("query vote info use cache: [{}]", voteId);
            VoteVO voteVO = fanTokenCacheHelper.queryVoteInfo(voteId);
            fanTokenI18nHelper.doVoteI18n(voteVO);
            recalculateVoteStatus(Collections.singletonList(voteVO));
            return new CommonRet<>(voteVO);
        } else {
            log.info("query vote info use db: [{}]", voteId);
            APIResponse<VoteVO> response = fanTokenVoteManageAPI.
                    queryVoteInfo(APIRequest.instance(QueryVoteInfoRequest.builder().voteId(voteId).
                            callerType(CallerTypeEnum.CUSTOM.getCallerType()).build()));
            baseHelper.checkResponse(response);
            fanTokenI18nHelper.doVoteI18n(response.getData());
            recalculateVoteStatus(Collections.singletonList(response.getData()));
            return new CommonRet<>(response.getData());
        }
    }

    /**
     * <h2>重新计算 Vote 的状态</h2>
     * 如果 vote 当前返回的状态是 open, 需要判断下 voteEndTime, 可能需要调整为 completed, 其他的状态不需要考虑
     * */
    private void recalculateVoteStatus(List<VoteVO> voteVOS) {

        if (CollectionUtils.isNotEmpty(voteVOS)) {
            long currentTime = System.currentTimeMillis();
            voteVOS.forEach(v -> {
                if (null != v.getVoteConfig()
                        && VoteStatusEnum.OPEN.getVoteStatus().equals(v.getVoteConfig().getVoteStatus())) {
                    if (null != v.getVoteConfig().getVoteEndTime()
                            && v.getVoteConfig().getVoteEndTime().getTime() <= currentTime) {
                        v.getVoteConfig().setVoteStatus(VoteStatusEnum.COMPLETED.getVoteStatus());
                    }
                }
            });
        }
    }

    @GetMapping("query-user-vote")
    public CommonRet<List<String>>queryUserVote(String voteId) throws Exception {
        Long userId = baseHelper.getUserId();
        log.info("fanTokenQueryUserVote,userId:{}",userId);
        if (null == userId){
            return new CommonRet<>();
        }
        APIResponse<QueryUserVoteResponse> response =
                fanTokenVoteManageAPI.queryUserVote(APIRequest.instance(QueryUserVoteRequest.builder().userId(userId).voteId(voteId).build()));
        baseHelper.checkResponse(response);
        return new CommonRet<>(response.getData().getOptionIds());
    }

    @GetMapping("query-vote-statistic")
    public CommonRet<List<OptionStatisticVO>>queryVoteStatistic(String voteId) throws Exception{
        APIResponse<QueryVoteStatisticResponse> response = fanTokenVoteManageAPI.
                queryVoteStatistic(APIRequest.instance(QueryVoteStatisticRequest.builder().voteId(voteId).build()));
        baseHelper.checkResponse(response);
        return new CommonRet<>(response.getData().getVoteStatistic());
    }

    @PostMapping("user-vote")
    public CommonRet<Void> userVote(@Valid @RequestBody UserVoteRequest request) throws Exception {

        Long userId = baseHelper.getUserId();
        log.info("fanTokenUserVote,userId:{}",userId);
        if (null == userId){
            return new CommonRet<>();
        }

        request.setUserId(userId);
        request.setComplianceAssetDto(fanTokenCheckHelper.fanTokenComplianceAsset(userId));
        request.setHasKyc(fanTokenCheckHelper.hasKyc(userId));
        request.setIp(IPUtils.getIp());
        request.setFvideoid(WebUtils.getHeader("fvideo-id"));

        APIResponse<UserVoteResponse> response = fanTokenVoteManageAPI.userVote(APIRequest.instance(request));
        baseHelper.checkResponse(response);
        return new CommonRet<>();
    }

    @PostMapping("query-voted-list")
    public CommonRet<List<String>>queryVotedList(@RequestBody QueryVotedListRequest request) throws Exception {
        Long userId = baseHelper.getUserId();
        if (null == userId){
            return new CommonRet<>();
        }
        request.setUserId(userId);
        APIResponse<QueryVotedListResponse> response =
                fanTokenVoteManageAPI.queryVotedList(APIRequest.instance(request));
        baseHelper.checkResponse(response);
        return new CommonRet<>(response.getData().getVotedIds());
    }

    /**
     * <h2>球队信息</h2>
     * */
    @GetMapping("/fantoken-vote-team")
    public CommonRet<List<SimpleTeamInfo>> fantokenVoteTeam() {

        boolean isGray = fanTokenCheckHelper.isGray();
        Long userId = baseHelper.getUserId();

        CommonQueryRequest request = CommonQueryRequest.builder()
                .isGray(isGray)
                .userId(userId)
                .clientType(baseHelper.getClientType())
                .build();

        APIResponse<List<SimpleTeamInfo>> response = fanTokenVoteManageAPI.fantokenVoteTeam(APIRequest.instance(request));
        baseHelper.checkResponse(response);
        List<SimpleTeamInfo> result = fanTokenCheckHelper.getGccComplianceTeamInfo(isGray, userId, response.getData());

        // i18n
        fanTokenI18nHelper.doSimpleTeamInfoList(result);

        return new CommonRet<>(result);
    }
}
