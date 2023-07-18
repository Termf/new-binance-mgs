package com.binance.mgs.nft.activity.controller.staking;

import com.binance.master.commons.SearchResult;
import com.binance.master.models.APIResponse;
import com.binance.mgs.nft.activity.helper.ActivityStakingHelper;
import com.binance.mgs.nft.activity.request.StakingAvailableAssetRequest;
import com.binance.mgs.nft.core.config.MgsNftProperties;
import com.binance.mgs.nft.nftasset.controller.helper.NftAssetHelper;
import com.binance.nft.activityservice.request.staking.*;
import com.binance.nft.activityservice.response.ApeStakeSearchResult;
import com.binance.nft.activityservice.response.staking.*;
import com.binance.nft.assetservice.api.data.dto.AvailableStakeNftDto;
import com.binance.platform.mgs.base.helper.BaseHelper;
import com.binance.platform.mgs.base.vo.CommonRet;
import io.swagger.annotations.Api;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.validation.Valid;
import java.util.List;

/**
 * @author joy
 * @date 2023/3/30 21:25
 */
@Api
@Slf4j
@RestController
@RequestMapping("/v1")
public class ActivityStakingController {
    @Resource
    private ActivityStakingHelper activityStakingHelper;
    @Resource
    private NftAssetHelper assetHelper;
    @Resource
    private BaseHelper baseHelper;
    @Resource
    private MgsNftProperties mgsProperties;

    @PostMapping("/friendly/nft/activity/staking/simple-info/{collectionId}")
    public CommonRet<StakingActivitySimpleResponse> queryStakingByCollection(@PathVariable Long collectionId) {
        Long userId = baseHelper.getUserId();
        StakingActivitySimpleResponse data = activityStakingHelper.getSimpleInfoByCollectionId(userId, collectionId);
        return new CommonRet<>(data);
    }

    @PostMapping("/friendly/nft/activity/staking/configs")
    public CommonRet<StakingActivityDetailResponse> stakingActivityDisplay(@RequestBody @Valid StakingActivityQueryRequest request) {
        Long userId = baseHelper.getUserId();
        request.setUserId(userId);
        StakingActivityDetailResponse data = activityStakingHelper.stakingDisplay(request);
        return new CommonRet<>(data);
    }

    @PostMapping("/public/nft/activity/staking/terms")
    public CommonRet<StakingActivityTermResponse> getStakingTerms(@RequestBody @Valid StakingActivityTermRequest request) {
        StakingActivityTermResponse data = activityStakingHelper.getActivityTerms(request);
        return new CommonRet<>(data);
    }

    @PostMapping("/private/nft/activity/staking/staked-nft")
    public CommonRet<ApeStakeSearchResult<StakingNftResponse>> getUserStakingPage(@RequestBody @Valid StakingUserStakePageRequest request) {
        Long userId = baseHelper.getUserId();
        request.setUserId(userId);
        ApeStakeSearchResult<StakingNftResponse> data = activityStakingHelper.getUserStakingPage(request);
        return new CommonRet<>(data);
    }

    @PostMapping("/private/nft/activity/staking/batch-stake")
    public CommonRet<Boolean> batchStake(@RequestBody @Valid StakingBatchStakeRequest request) {
        Long userId = baseHelper.getUserId();
        request.setUserId(userId);
        Boolean data = activityStakingHelper.batchStake(request);
        return new CommonRet<>(data);
    }

    @PostMapping("/private/nft/activity/staking/batch-cancel")
    public CommonRet<Boolean> batchCancel(@RequestBody @Valid StakingBatchCancelRequest request) {
        Long userId = baseHelper.getUserId();
        request.setUserId(userId);
        Boolean data = activityStakingHelper.batchCancel(request);
        return new CommonRet<>(data);
    }

    @PostMapping("/private/nft/activity/staking/batch-unstake")
    public CommonRet<Boolean> batchCancel(@RequestBody @Valid StakingBatchUnstakelRequest request) {
        Long userId = baseHelper.getUserId();
        request.setUserId(userId);
        Boolean data = activityStakingHelper.batchUnstake(request);
        return new CommonRet<>(data);
    }

    @PostMapping("/private/nft/activity/staking/get-available-nft")
    public CommonRet<List<AvailableStakeNftDto>> getAvailableStakeNft(@RequestBody StakingAvailableAssetRequest request) {
        StakingActivitySimpleResponse activity = activityStakingHelper.getSimpleInfoByActivityId(request.getActivityId());
        APIResponse<List<AvailableStakeNftDto>> response = assetHelper.getAvailableStakeNftV2(activity.getCollectionId(), baseHelper.getUserId());
        return new CommonRet<>(response.getData());
    }

    @PostMapping("/private/nft/activity/staking/rewards-history")
    public CommonRet<SearchResult<StakingRewardsResponse>> getRewardsHistory(@RequestBody StakingRewardsQueryRequest request) {
        request.setUserId(baseHelper.getUserId());
        SearchResult<StakingRewardsResponse> response = activityStakingHelper.getRewardsHistory(request);
        return new CommonRet<>(response);
    }
}
