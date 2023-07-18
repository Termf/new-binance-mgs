package com.binance.mgs.nft.activity.controller;

import com.alibaba.fastjson.JSON;
import com.binance.master.models.APIRequest;
import com.binance.master.models.APIResponse;
import com.binance.mgs.nft.activity.request.AvailableStakeAssetRequest;
import com.binance.mgs.nft.activity.response.ApeActivitySwitch;
import com.binance.mgs.nft.activity.service.ActivityService;
import com.binance.mgs.nft.nftasset.controller.helper.NftAssetHelper;
import com.binance.nft.activityservice.api.NftActivityStackeApi;
import com.binance.nft.activityservice.dto.ApeProjectDTO;
import com.binance.nft.activityservice.dto.StakingNftDTO;
import com.binance.nft.activityservice.request.ApeStakePageRequest;
import com.binance.nft.activityservice.request.ProjectRequest;
import com.binance.nft.activityservice.request.StakeRequest;
import com.binance.nft.activityservice.response.ApeStakeSearchResult;
import com.binance.nft.assetservice.api.data.dto.AvailableStakeNftDto;
import com.binance.nft.bnbgtwservice.api.data.dto.CollectionApyDTO;
import com.binance.nft.bnbgtwservice.api.data.dto.CommonPageRet;
import com.binance.nft.bnbgtwservice.api.data.dto.NftRewardRecordDto;
import com.binance.nft.bnbgtwservice.api.data.dto.NftStakingPreviewDto;
import com.binance.nft.bnbgtwservice.api.data.req.NftRewardHistoryArg;
import com.binance.nft.bnbgtwservice.api.data.req.NftStakingReviewArg;
import com.binance.nft.bnbgtwservice.api.iface.StakeApi;
import com.binance.platform.mgs.base.helper.BaseHelper;
import com.binance.platform.mgs.base.vo.CommonRet;
import com.ctrip.framework.apollo.spring.annotation.ApolloJsonValue;
import com.google.common.collect.Maps;
import io.swagger.annotations.Api;
import lombok.extern.slf4j.Slf4j;
import ma.glasnost.orika.MapEntry;
import org.apache.commons.collections4.MapUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @author: felix
 * @date: 21.10.22
 * @description:
 */
@Api
@Slf4j
@RestController
@RequestMapping("/v1")
public class StakeController {
    @Resource
    private ActivityService activityService;
    @Resource
    private NftActivityStackeApi activityStackeApi;

    @Resource
    private NftAssetHelper assetHelper;

    @Resource
    private BaseHelper baseHelper;

    @Resource
    private StakeApi stakeApi;

    @Value("${nft.ape.switch:true}")
    private Boolean apeSwitch;

    @Value("${nft.ape.RoAE:21%}")
    private String apeRoAE;

    @ApolloJsonValue("${ape.stake.nft.feName:{\"BAYC\":\"BAYC\",\"MAYC\":\"bored_ape_mayc_collection\",\"BAKC\":\"bored_ape_bakc_collection\"}}")
    private Map<String, String> feName2CollectionName;

    @GetMapping("/public/nft/activity/getApyByCollection")
    public CommonRet<Map<String,CollectionApyDTO>> getApyByCollection() throws Exception {
        Map<String, CollectionApyDTO> collection2Apy = activityService.getApyByCollection();
        return new CommonRet<>(collection2Apy);
    }

    @PostMapping("/private/nft/activity/ape-staking-page")
    public CommonRet<ApeStakeSearchResult<StakingNftDTO>> apeStkingPage(@RequestBody ApeStakePageRequest request) throws Exception {
        log.error("feName2CollectionName is {} ", JSON.toJSON(feName2CollectionName));
        request.setFeCollection(request.getCollection());
        request.setCollection(feName2CollectionName.get(request.getCollection()));
        request.setUserId(baseHelper.getUserId());
        APIResponse<ApeStakeSearchResult<StakingNftDTO>> response = activityStackeApi.apeStakePage(APIRequest.instance(request));
        return new CommonRet<>(response.getData());
    }

    @PostMapping("/private/nft/activity/ape-stake")
    public CommonRet<Boolean> apeStake(@RequestBody StakeRequest request) throws Exception {
        request.setUserId(baseHelper.getUserId());
        Map<String,String> collectionName2feNameMap = feName2CollectionName.entrySet().stream().collect(Collectors.toMap(Map.Entry::getValue, Map.Entry::getKey));
        request.setCollectionName2feName(collectionName2feNameMap);
        APIResponse<Boolean> response = activityStackeApi.apeStake(APIRequest.instance(request));
        baseHelper.checkResponse(response);
        return new CommonRet<>(response.getData());
    }

    @PostMapping("/private/nft/activity/ape-unstake")
    public CommonRet<Boolean> apeUnStake(@RequestBody StakeRequest request) throws Exception {
        request.setUserId(baseHelper.getUserId());
        APIResponse<Boolean> response = activityStackeApi.apeUnstake(APIRequest.instance(request));
        return new CommonRet<>(response.getData());
    }

    @PostMapping("/private/nft/activity/getAvailableStakeNft")
    public CommonRet<List<AvailableStakeNftDto>> getAvailableStakeNft(@RequestBody AvailableStakeAssetRequest request) throws Exception {
        log.error("collectionName1,{}",request.getCollectionName());
        request.setCollectionName(feName2CollectionName.get(request.getCollectionName()));
        log.error("collectionName2,{}",request.getCollectionName());
        APIResponse<List<AvailableStakeNftDto>> response = assetHelper.getAvailableStakeNft(request.getCollectionName(),baseHelper.getUserId());
        return new CommonRet<>(response.getData());
    }

    @GetMapping("/public/nft/activity/apeSwitch")
    public CommonRet<ApeActivitySwitch> apeSwitch() throws Exception {
        return new CommonRet<>(ApeActivitySwitch.builder().isDisplay(apeSwitch).RoAE(apeRoAE).build());
    }
    @PostMapping("/private/nft/activity/stake-preview")
    public CommonRet<NftStakingPreviewDto> stakePreview(@RequestBody NftStakingReviewArg request) throws Exception {
        APIResponse<NftStakingPreviewDto> response = stakeApi.stakePreview(APIRequest.instance(request));
        return new CommonRet<>(response.getData());
    }

    @PostMapping("/private/nft/activity/reward-history")
    public CommonRet<CommonPageRet<NftRewardRecordDto>> rewardHistory(@RequestBody NftRewardHistoryArg request) throws Exception {
        request.setUserId(baseHelper.getUserId());
        APIResponse<CommonPageRet<NftRewardRecordDto>> response = stakeApi.rewardHistory(APIRequest.instance(request));
        return new CommonRet<>(response.getData());
    }
    @PostMapping("/private/nft/activity/project-list")
    public CommonRet<List<ApeProjectDTO>> projectList(@RequestBody ProjectRequest request) throws Exception {
        APIResponse<List<ApeProjectDTO>> response = activityStackeApi.projectList(APIRequest.instance(request));
        baseHelper.checkResponse(response);
        List<Long> projectList = response.getData().stream().map(ApeProjectDTO::getId).collect(Collectors.toList());
        return new CommonRet<>(activityService.projectList(projectList));
    }

}
