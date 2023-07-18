package com.binance.mgs.nft.fantoken.controller;

import com.alibaba.fastjson.JSON;
import com.binance.master.models.APIRequest;
import com.binance.master.models.APIResponse;
import com.binance.mgs.nft.fantoken.helper.FanTokenCheckHelper;
import com.binance.nft.bnbgtwservice.api.data.dto.FanTokenComplianceAssetDto;
import com.binance.platform.mgs.base.helper.BaseHelper;
import com.binance.platform.mgs.base.vo.CommonRet;
import com.binance.nft.fantoken.ifae.IFanTokenAssetManageAPI;
import com.binance.nft.fantoken.request.UserAssetRequest;
import com.binance.nft.fantoken.response.UserAssetResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Slf4j
@RequestMapping("/v1/private/nft/fantoken")
@RestController
@RequiredArgsConstructor
public class FanTokenAssetController {

    private final BaseHelper baseHelper;
    private final IFanTokenAssetManageAPI assetManageAPI;
    private final FanTokenCheckHelper fanTokenCheckHelper;

    @PostMapping("/asset")
    public CommonRet<UserAssetResponse> queryUserAssetInfo(@RequestBody UserAssetRequest request) {

        Long userId = baseHelper.getUserId();
        if (null == userId) {
            return new CommonRet<>();
        }

        boolean isGrey = fanTokenCheckHelper.isGray();

        // 生产环境下 gcc compliance 校验
        if (!isGrey) {
            FanTokenComplianceAssetDto complianceAsset = fanTokenCheckHelper.fanTokenComplianceAsset(userId);
            if (null != complianceAsset && !complianceAsset.getPass()) {
                List<String> assets = complianceAsset.getAssets();
                // 如果不合规, 设置为空
                if (CollectionUtils.isEmpty(assets) || !assets.contains(request.getAsset())) {
                    log.error("user asset query trigger gcc compliance: [{}], [{}]", JSON.toJSONString(complianceAsset),
                            JSON.toJSONString(request));
                    return new CommonRet<>();
                }
            }
        }

        request.setUserId(userId);
        APIResponse<UserAssetResponse> response = assetManageAPI.userAsset(APIRequest.instance(request));
        baseHelper.checkResponse(response);
        return new CommonRet<>(response.getData());
    }
}
