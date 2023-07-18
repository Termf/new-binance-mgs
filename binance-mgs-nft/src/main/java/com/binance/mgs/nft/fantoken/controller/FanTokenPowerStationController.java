package com.binance.mgs.nft.fantoken.controller;

import com.alibaba.fastjson.JSON;
import com.binance.master.models.APIRequest;
import com.binance.master.models.APIResponse;
import com.binance.platform.mgs.base.helper.BaseHelper;
import com.binance.platform.mgs.base.vo.CommonRet;
import com.binance.mgs.nft.fantoken.helper.CacheProperty;
import com.binance.mgs.nft.fantoken.helper.FanTokenCacheHelper;
import com.binance.mgs.nft.fantoken.helper.FanTokenCheckHelper;
import com.binance.mgs.nft.mysterybox.helper.FanTokenI18nHelper;
import com.binance.nft.fantoken.ifae.IFanTokenNftStakingManageApi;
import com.binance.nft.fantoken.request.CommonPageRequest;
import com.binance.nft.fantoken.request.nftstaking.ChooseNftRequest;
import com.binance.nft.fantoken.request.nftstaking.NftStakingDisplayRequest;
import com.binance.nft.fantoken.request.nftstaking.StakingHistoryRequest;
import com.binance.nft.fantoken.request.nftstaking.StakingIdRequest;
import com.binance.nft.fantoken.response.CommonPageResponse;
import com.binance.nft.fantoken.response.VoidResponse;
import com.binance.nft.fantoken.response.nftstaking.CalculationHistoryResponse;
import com.binance.nft.fantoken.response.nftstaking.DistributionHistoryResponse;
import com.binance.nft.fantoken.response.nftstaking.NftStakingDisplayResponse;
import com.binance.nft.fantoken.response.nftstaking.StakingHistoryResponse;
import com.binance.nft.fantoken.response.nftstaking.UsableNftResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * <h1>Nft Staking</h1>
 * Staking 响应中增加 ruleLink、tutorialLink 字段
 * 2022.05.10 支持 Regular NFT
 * */
@Slf4j
@RequestMapping("/v1/friendly/nft/fantoken/staking")
@RestController
@RequiredArgsConstructor
public class FanTokenPowerStationController {

    private final BaseHelper baseHelper;
    private final CacheProperty cacheProperty;
    private final FanTokenI18nHelper fanTokenI18nHelper;
    private final FanTokenCacheHelper fanTokenCacheHelper;
    private final FanTokenCheckHelper fanTokenCheckHelper;
    private final IFanTokenNftStakingManageApi nftStakingManageApi;

    @PostMapping("/display")
    public CommonRet<NftStakingDisplayResponse> stakingDisplay(@RequestBody NftStakingDisplayRequest request) throws Exception {

        Long userId = baseHelper.getUserId();
        request.setUserId(userId);  // 可以是 null
        log.info("power-station display: [{}]", JSON.toJSONString(request));

        if (null == userId && cacheProperty.isEnabled()) {
            // 没有登录, 使用 Guava Cache, 1 分钟缓存
            NftStakingDisplayResponse result = fanTokenCacheHelper.queryNftStakingDisplay(request);
            fanTokenI18nHelper.doNftStakingDisplayI18n(result);
            return new CommonRet<>(result);
        } else {
            // 是登录状态
            APIResponse<NftStakingDisplayResponse> response =
                    nftStakingManageApi.stakingDisplay(APIRequest.instance(request));
            baseHelper.checkResponse(response);
            fanTokenI18nHelper.doNftStakingDisplayI18n(response.getData());
            return new CommonRet<>(response.getData());
        }
    }

    @PostMapping("/distribution-history")
    public CommonRet<CommonPageResponse<DistributionHistoryResponse>> stakingClaimHistory(
            @RequestBody CommonPageRequest<StakingIdRequest> request) {

        Long userId = baseHelper.getUserId();
        if (null == userId) {
            throw new IllegalArgumentException("user not login!");
        }

        // 强制 KYC 的校验
        fanTokenCheckHelper.userComplianceValidate(userId);

        request.getParams().setUserId(userId);
        log.info("power-station stakingClaimHistory: [{}]", JSON.toJSONString(request));

        APIResponse<CommonPageResponse<DistributionHistoryResponse>> response =
                nftStakingManageApi.stakingClaimHistory(APIRequest.instance(request));
        baseHelper.checkResponse(response);
        fanTokenI18nHelper.doStakingClaimHistory(response.getData().getData());
        return new CommonRet<>(response.getData());
    }

    @PostMapping("/calculation-history")
    public CommonRet<CommonPageResponse<CalculationHistoryResponse>> stakingCalculationHistory(
            @RequestBody CommonPageRequest<StakingIdRequest> request) {

        Long userId = baseHelper.getUserId();
        if (null == userId) {
            throw new IllegalArgumentException("user not login!");
        }

        // 强制 KYC 的校验
        fanTokenCheckHelper.userComplianceValidate(userId);

        request.getParams().setUserId(userId);
        log.info("power-station stakingCalculationHistory: [{}]", JSON.toJSONString(request));

        APIResponse<CommonPageResponse<CalculationHistoryResponse>> response =
                nftStakingManageApi.stakingCalculationHistory(APIRequest.instance(request));
        baseHelper.checkResponse(response);
        fanTokenI18nHelper.doStakingCalculationHistory(response.getData().getData());
        return new CommonRet<>(response.getData());
    }

    @PostMapping("/usable-nft")
    public CommonRet<CommonPageResponse<UsableNftResponse>> stakingUsableNft(
            @RequestBody CommonPageRequest<StakingIdRequest> request) {

        Long userId = baseHelper.getUserId();
        if (null == userId) {
            throw new IllegalArgumentException("user not login!");
        }

        // 强制 KYC 的校验
        fanTokenCheckHelper.userComplianceValidate(userId);

        request.getParams().setUserId(userId);
        request.getParams().setComplianceAssetDto(fanTokenCheckHelper.fanTokenComplianceAsset(baseHelper.getUserId()));
        log.info("power-station stakingUsableNft: [{}]", JSON.toJSONString(request));

        APIResponse<CommonPageResponse<UsableNftResponse>> response =
                nftStakingManageApi.stakingUsableNft(APIRequest.instance(request));
        baseHelper.checkResponse(response);
        fanTokenI18nHelper.doStakingUsableNft(response.getData().getData());
        return new CommonRet<>(response.getData());
    }

    @PostMapping("/choose-nft")
    public CommonRet<VoidResponse> stakingChooseNft(@RequestBody ChooseNftRequest request) {

        Long userId = baseHelper.getUserId();
        if (null == userId) {
            throw new IllegalArgumentException("user not login!");
        }

        // 强制 KYC 的校验
        fanTokenCheckHelper.userComplianceValidate(userId);

        request.setUserId(userId);
        request.setComplianceAssetDto(fanTokenCheckHelper.fanTokenComplianceAsset(baseHelper.getUserId()));
        log.info("power-station stakingChooseNft: [{}]", JSON.toJSONString(request));

        APIResponse<VoidResponse> response = nftStakingManageApi.stakingChooseNft(APIRequest.instance(request));
        baseHelper.checkResponse(response);
        return new CommonRet<>(response.getData());
    }

    @PostMapping("/unstake-nft")
    public CommonRet<VoidResponse> stakingUnstakeNft(@RequestBody ChooseNftRequest request) {

        Long userId = baseHelper.getUserId();
        if (null == userId) {
            throw new IllegalArgumentException("user not login!");
        }

        // 强制 KYC 的校验
        fanTokenCheckHelper.userComplianceValidate(userId);

        request.setUserId(userId);
        request.setComplianceAssetDto(fanTokenCheckHelper.fanTokenComplianceAsset(baseHelper.getUserId()));
        log.info("power-station stakingUnstakeNft: [{}]", JSON.toJSONString(request));

        APIResponse<VoidResponse> response = nftStakingManageApi.stakingUnstakeNft(APIRequest.instance(request));
        baseHelper.checkResponse(response);
        return new CommonRet<>(response.getData());
    }

    @PostMapping("/claim-rewards")
    public CommonRet<VoidResponse> stakingClaimRewards(@RequestBody StakingIdRequest request) {

        Long userId = baseHelper.getUserId();
        if (null == userId) {
            throw new IllegalArgumentException("user not login!");
        }

        // 强制 KYC 的校验
        fanTokenCheckHelper.userComplianceValidate(userId);

        request.setUserId(userId);
        request.setComplianceAssetDto(fanTokenCheckHelper.fanTokenComplianceAsset(baseHelper.getUserId()));
        log.info("power-station stakingClaimRewards: [{}]", JSON.toJSONString(request));

        APIResponse<VoidResponse> response = nftStakingManageApi.stakingClaimRewards(APIRequest.instance(request));
        baseHelper.checkResponse(response);
        return new CommonRet<>(response.getData());
    }

    @PostMapping("/staking-history")
    public CommonRet<CommonPageResponse<StakingHistoryResponse>> stakingStakingHistory(
            @RequestBody CommonPageRequest<StakingHistoryRequest> request) {

        Long userId = baseHelper.getUserId();
        // 如果是登录用户, 则填充用户信息到请求中
        if (null != userId) {
            request.getParams().setUserId(userId);
            log.info("power-station stakingStakingHistory: [{}]", JSON.toJSONString(request));
            // 强制 KYC 的校验
            fanTokenCheckHelper.userComplianceValidate(userId);
        }

        APIResponse<CommonPageResponse<StakingHistoryResponse>> response =
                nftStakingManageApi.stakingStakingHistory(APIRequest.instance(request));
        baseHelper.checkResponse(response);
        fanTokenI18nHelper.doStakingStakingHistory(response.getData().getData());
        return new CommonRet<>(response.getData());
    }
}
