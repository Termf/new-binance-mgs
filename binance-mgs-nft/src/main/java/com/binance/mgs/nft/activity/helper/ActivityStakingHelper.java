package com.binance.mgs.nft.activity.helper;

import com.binance.master.commons.SearchResult;
import com.binance.master.models.APIRequest;
import com.binance.master.models.APIResponse;
import com.binance.mgs.nft.core.config.MgsNftProperties;
import com.binance.nft.activityservice.api.staking.NftStakingApi;
import com.binance.nft.activityservice.request.staking.*;
import com.binance.nft.activityservice.response.ApeStakeSearchResult;
import com.binance.nft.activityservice.response.staking.*;
import com.binance.platform.mgs.base.helper.BaseHelper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * @author joy
 * @date 2023/3/30 22:42
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ActivityStakingHelper {
    private final NftStakingApi nftStakingApi;
    private final BaseHelper baseHelper;
    private final MgsNftProperties mgsNftProperties;

    public StakingActivitySimpleResponse getSimpleInfoByCollectionId(Long userId,Long collectionId) {
        Long targetCollectionId = mgsNftProperties.getStakingCollectionMapping().getOrDefault(collectionId, collectionId);
        StakingCollectionQueryRequest request = StakingCollectionQueryRequest.builder()
                .userId(userId)
                .collectionId(targetCollectionId)
                .build();
        APIResponse<StakingActivitySimpleResponse> response = nftStakingApi.getStakingActivityByCollectionId(APIRequest.instance(request));
        baseHelper.checkResponse(response);
        return response.getData();
    }

    public StakingActivitySimpleResponse getSimpleInfoByActivityId(String activityId) {
        APIResponse<StakingActivitySimpleResponse> response = nftStakingApi.getStakingActivityByActivityId(APIRequest.instance(activityId));
        baseHelper.checkResponse(response);
        return response.getData();
    }

    public StakingActivityDetailResponse stakingDisplay(StakingActivityQueryRequest request) {
        APIResponse<StakingActivityDetailResponse> response = nftStakingApi.getStakingDisplay(APIRequest.instance(request));
        baseHelper.checkResponse(response);
        return response.getData();
    }

    public StakingActivityTermResponse getActivityTerms(StakingActivityTermRequest request) {
        APIResponse<StakingActivityTermResponse> response = nftStakingApi.getStakingTerms(APIRequest.instance(request));
        baseHelper.checkResponse(response);
        return response.getData();
    }

    public ApeStakeSearchResult<StakingNftResponse> getUserStakingPage(StakingUserStakePageRequest request) {
        APIResponse<ApeStakeSearchResult<StakingNftResponse>> response = nftStakingApi.userStakingPage(APIRequest.instance(request));
        baseHelper.checkResponse(response);
        return response.getData();
    }

    public Boolean batchStake(StakingBatchStakeRequest request) {
        APIResponse<Boolean> response = nftStakingApi.batchStake(APIRequest.instance(request));
        baseHelper.checkResponse(response);
        return response.getData();
    }

    public Boolean batchCancel(StakingBatchCancelRequest request) {
        APIResponse<Boolean> response = nftStakingApi.batchCancel(APIRequest.instance(request));
        baseHelper.checkResponse(response);
        return response.getData();
    }

    public Boolean batchUnstake(StakingBatchUnstakelRequest request) {
        APIResponse<Boolean> response = nftStakingApi.batchUnstake(APIRequest.instance(request));
        baseHelper.checkResponse(response);
        return response.getData();
    }

    public SearchResult<StakingRewardsResponse> getRewardsHistory(StakingRewardsQueryRequest request) {
        APIResponse<SearchResult<StakingRewardsResponse>> response = nftStakingApi.rewardsHistory(APIRequest.instance(request));
        baseHelper.checkResponse(response);
        return response.getData();
    }
}
