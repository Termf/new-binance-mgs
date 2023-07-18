package com.binance.mgs.nft.nftasset.controller.helper;

import com.binance.master.models.APIRequest;
import com.binance.master.models.APIResponse;
import com.binance.master.utils.CopyBeanUtils;
import com.binance.nft.assetservice.api.data.request.NftApproveRequest;
import com.binance.platform.mgs.base.helper.BaseHelper;
import com.binance.nft.assetservice.api.IUserApproveApi;
import com.binance.nft.assetservice.api.data.request.ProductApproveRequest;
import com.binance.nft.assetservice.api.data.vo.ItemsApproveInfo;
import com.binance.nft.market.vo.UserApproveInfo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class ApproveHelper {

    private final IUserApproveApi userApproveApi;

    private final BaseHelper baseHelper;

    public List<ItemsApproveInfo> queryApproveInfo(List<Long> productIdList, Long userId) {

        if (CollectionUtils.isEmpty(productIdList)) {
            return Collections.emptyList();
        }
        ProductApproveRequest request = ProductApproveRequest.builder()
                .productIdList(productIdList).userId(userId).build();
        APIResponse<List<ItemsApproveInfo>> response = userApproveApi
                .productApproveList(APIRequest.instance(request));
        baseHelper.checkResponse(response);
        return response.getData();
    }

    public Map<Long, ItemsApproveInfo> queryApproveInfoMap(List<Long> productIdList, Long userId) {

        List<ItemsApproveInfo> infoList = this.queryApproveInfo(productIdList, userId);
        return Optional.of(infoList)
                .orElse(Collections.emptyList())
                .stream().collect(Collectors.toMap(ItemsApproveInfo::getProductId, Function.identity(), (v1, v2) -> v1));
    }

    public UserApproveInfo queryApproveInfo(Long productId, Long userId) {

        List<ItemsApproveInfo> infoList = this.queryApproveInfo(Arrays.asList(productId), userId);
        if(CollectionUtils.isEmpty(infoList)) {
            return null;
        }

        return CopyBeanUtils.fastCopy(infoList.get(0), UserApproveInfo.class);
    }

    public Map<Long, ItemsApproveInfo> queryApproveInfoMapByNftId(List<Long> nftIdList, Long userId) {

        List<ItemsApproveInfo> infoList = this.queryApproveInfoByNftId(nftIdList, userId);
        return Optional.of(infoList)
                .orElse(Collections.emptyList())
                .stream().collect(Collectors.toMap(ItemsApproveInfo::getNftId, Function.identity(), (v1, v2) -> v1));
    }

    private List<ItemsApproveInfo> queryApproveInfoByNftId(List<Long> nftIdList, Long userId) {
        if (CollectionUtils.isEmpty(nftIdList)) {
            return Collections.emptyList();
        }
        NftApproveRequest request = NftApproveRequest.builder()
                .nftInfoIdList(nftIdList).userId(userId).build();
        APIResponse<List<ItemsApproveInfo>> response = userApproveApi
                .nftApproveList(APIRequest.instance(request));
        baseHelper.checkResponse(response);
        return response.getData();
    }
}
