package com.binance.mgs.nft.activity.controller;

import com.binance.master.commons.SearchResult;
import com.binance.master.models.APIRequest;
import com.binance.master.models.APIResponse;
import com.binance.mgs.nft.activity.response.BurnNftInfoQueryResponse;
import com.binance.nft.activityservice.api.BurnNftApi;
import com.binance.nft.activityservice.request.BurnHistoryRequest;
import com.binance.nft.activityservice.request.BurnNftRequest;
import com.binance.nft.activityservice.vo.BurnHistoryItemVo;
import com.binance.nft.assetservice.api.INftInfoApi;
import com.binance.nft.assetservice.api.data.request.SelectUserNftByCollectionRequest;
import com.binance.nft.assetservice.api.data.vo.NftInfoDto;
import com.binance.platform.mgs.base.helper.BaseHelper;
import com.binance.platform.mgs.base.vo.CommonRet;
import io.swagger.annotations.Api;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.validation.Valid;
import java.util.List;
import java.util.stream.Collectors;

@Api
@Slf4j
@RestController
@RequestMapping("/v1")
public class BurnNftController {
    @Resource
    private INftInfoApi iNftInfoApi;
    @Resource
    private BurnNftApi burnNftApi;
    @Resource
    private BaseHelper baseHelper;


    @PostMapping("/private/nft/activity/burn-nft")
    public CommonRet<Void> burnNft(@Valid @RequestBody BurnNftRequest request) throws Exception {
        request.setUserId(baseHelper.getUserId());
        APIResponse<Void> response = burnNftApi.burnNft(APIRequest.instance(request));
        baseHelper.checkResponse(response);
        return new CommonRet<>();
    }

    @PostMapping("/private/nft/activity/burn-history")
    public CommonRet<SearchResult<BurnHistoryItemVo>> burnHistory(@Valid @RequestBody BurnHistoryRequest request) throws Exception {
        request.setUserId(baseHelper.getUserId());
        APIResponse<SearchResult<BurnHistoryItemVo>> response = burnNftApi.burnHistory(APIRequest.instance(request));
        baseHelper.checkResponse(response);
        return new CommonRet<>(response.getData());
    }

    @GetMapping("/private/nft/activity/collection-query")
    public CommonRet<BurnNftInfoQueryResponse> collectionQuery(Long collectionId) throws Exception {
        APIResponse<List<NftInfoDto>> response = iNftInfoApi.selectUserNftByCollection(APIRequest.instance(
                SelectUserNftByCollectionRequest.builder().collectionId(collectionId).userId(baseHelper.getUserId()).build()));
        baseHelper.checkResponse(response);
        List<BurnNftInfoQueryResponse.BurnNftInfoVo> items = null;
        if(CollectionUtils.isNotEmpty(response.getData())) {
            items = response.getData().stream()
                    .map(item -> BurnNftInfoQueryResponse.BurnNftInfoVo.builder()
                            .coverUrl(item.getZippedUrl())
                            .title(item.getNftTitle())
                            .contractAddress(item.getContractAddress())
                            .tokenId(item.getTokenId())
                            .nftId(item.getId())
                            .nftType(item.getNftType().intValue())
                            .build()
                    ).collect(Collectors.toList());
        }
        return new CommonRet<>(new BurnNftInfoQueryResponse(items));
    }
}
