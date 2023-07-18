package com.binance.mgs.nft.fantoken.controller;

import com.binance.master.models.APIRequest;
import com.binance.master.models.APIResponse;
import com.binance.master.utils.StringUtils;
import com.binance.nft.bnbgtwservice.api.data.dto.FanTokenComplianceAssetDto;
import com.binance.platform.mgs.base.helper.BaseHelper;
import com.binance.platform.mgs.base.vo.CommonRet;
import com.binance.mgs.nft.fantoken.helper.CacheProperty;
import com.binance.mgs.nft.fantoken.helper.FanTokenCacheHelper;
import com.binance.mgs.nft.fantoken.helper.FanTokenCheckHelper;
import com.binance.nft.fantoken.ifae.IFanTokenMainPageConfigManageAPI;
import com.binance.nft.fantoken.request.QueryMainPageConfigRequest;
import com.binance.nft.fantoken.response.QuickActionResponse;
import com.binance.nft.fantoken.vo.MainPageConfigVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

@Slf4j
@RequestMapping("/v1/public/nft/fantoken")
@RestController
@RequiredArgsConstructor
public class FanTokenMainPageConfigController {

    private final CacheProperty cacheProperty;
    private final FanTokenCacheHelper fanTokenCacheHelper;
    private final BaseHelper baseHelper;
    private final IFanTokenMainPageConfigManageAPI fanTokenMainPageConfigManageAPI;
    private final FanTokenCheckHelper fanTokenCheckHelper;

    @GetMapping("/query-main-page-config")
    public CommonRet<MainPageConfigVO> queryMainPageConfig() throws Exception {

        // 在灰度环境下, 走独立的逻辑
        if (fanTokenCheckHelper.isGray()) {
            log.info("query main page config in grey");
            QueryMainPageConfigRequest request = QueryMainPageConfigRequest.builder().isGrey(true).build();
            APIResponse<MainPageConfigVO> response =
                    fanTokenMainPageConfigManageAPI.queryMainPageConfigWithTag(APIRequest.instance(request));
            baseHelper.checkResponse(response);
            return new CommonRet<>(response.getData());
        }

        // use cache
        if (cacheProperty.isEnabled()) {
            log.info("query main page config use cache");
            MainPageConfigVO configVO = fanTokenCacheHelper.queryMainPageConfig();
            processMainPageConfigForGccCompliance(configVO);
            return new CommonRet<>(configVO);
        } else {
            log.info("query main page config use db");
            APIResponse<MainPageConfigVO> response = fanTokenMainPageConfigManageAPI.queryMainPageConfig();
            baseHelper.checkResponse(response);
            processMainPageConfigForGccCompliance(response.getData());
            return new CommonRet<>(response.getData());
        }
    }

    private void processMainPageConfigForGccCompliance(MainPageConfigVO configVO) {

        // 只有生产环境需要校验
        FanTokenComplianceAssetDto complianceAsset = fanTokenCheckHelper.fanTokenComplianceAsset(baseHelper.getUserId());
        if (null != complianceAsset && !complianceAsset.getPass()) {
            // 允许的币种
            List<String> assets = complianceAsset.getAssets();
            // buySupportFanToken
            if (StringUtils.isNotBlank(configVO.getBuySupportFanToken())) {
                configVO.setBuySupportFanToken(getLegalSupportFanToken(assets, configVO.getBuySupportFanToken()));
            }
            // tradeSupportPair
            if (StringUtils.isNotBlank(configVO.getTradeSupportPair())) {
                List<String> legalTradeSupportPairs = new ArrayList<>();
                Stream.of(configVO.getTradeSupportPair().split(",")).forEach(p -> {
                    String[] tokens = p.split("/");
                    if (tokens.length == 2 && (assets.contains(tokens[0]) && assets.contains(tokens[1]))) {
                        legalTradeSupportPairs.add(p);
                    }
                });
                configVO.setTradeSupportPair(String.join(",", legalTradeSupportPairs));
            }
            // stakingSupportFanToken
            if (StringUtils.isNotBlank(configVO.getStakingSupportFanToken())) {
                configVO.setStakingSupportFanToken(getLegalSupportFanToken(assets, configVO.getStakingSupportFanToken()));
            }
            // supportFanToken
            if (StringUtils.isNotBlank(configVO.getSupportFanToken())) {
                configVO.setSupportFanToken(getLegalSupportFanToken(assets, configVO.getSupportFanToken()));
            }
        }
    }

    private String getLegalSupportFanToken(List<String> assets, String fantoken) {

        List<String> legalSupportFanTokens = new ArrayList<>(assets.size());
        Stream.of(fantoken.split(",")).forEach(f -> {
            if (assets.contains(f)) {
                legalSupportFanTokens.add(f);
            }
        });
        return String.join(",", legalSupportFanTokens);
    }

    @GetMapping("/quick-actions")
    public CommonRet<List<QuickActionResponse>> queryQuickActionByMainPageConfig() throws Exception {

        // 在灰度环境下, 走独立的逻辑
        if (fanTokenCheckHelper.isGray()) {
            log.info("queryQuickActionByMainPageConfig in grey");
            QueryMainPageConfigRequest request = QueryMainPageConfigRequest.builder().isGrey(true).build();
            APIResponse<List<QuickActionResponse>> response =
                    fanTokenMainPageConfigManageAPI.queryQuickActionByMainPageConfig(APIRequest.instance(request));
            baseHelper.checkResponse(response);
            return new CommonRet<>(response.getData());
        }

        // use cache
        if (cacheProperty.isEnabled()) {
            return new CommonRet<>(processQuickActionResponseForGccCompliance(fanTokenCacheHelper.queryQuickAction()));
        } else {
            QueryMainPageConfigRequest request = QueryMainPageConfigRequest.builder().isGrey(false).build();
            APIResponse<List<QuickActionResponse>> response =
                    fanTokenMainPageConfigManageAPI.queryQuickActionByMainPageConfig(APIRequest.instance(request));
            baseHelper.checkResponse(response);
            return new CommonRet<>(processQuickActionResponseForGccCompliance(response.getData()));
        }
    }

    private List<QuickActionResponse> processQuickActionResponseForGccCompliance(List<QuickActionResponse> actionResponses) {

        // 只有生产环境需要校验
        FanTokenComplianceAssetDto complianceAsset = fanTokenCheckHelper.fanTokenComplianceAsset(baseHelper.getUserId());
        if (null != complianceAsset && !complianceAsset.getPass()) {
            List<QuickActionResponse> result = new ArrayList<>(actionResponses.size());
            // 允许的币种
            List<String> assets = complianceAsset.getAssets();
            actionResponses.forEach(a -> {
                if (assets.contains(a.getToken())) {
                    result.add(a);
                }
            });
            return result;
        } else {
            return actionResponses;
        }
    }
}
