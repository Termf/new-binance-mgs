package com.binance.mgs.nft.trade;

import com.binance.master.models.APIRequest;
import com.binance.master.models.APIResponse;
import com.binance.mgs.nft.trade.proxy.TradeCacheProxy;
import com.binance.mgs.nft.trade.request.QueryAssetbyCollectionIdRequest;
import com.binance.mgs.nft.trade.response.ItemCollectionVo;
import com.binance.mgs.nft.trade.response.QueryAssetbyCollectionIdResponse;
import com.binance.nft.assetservice.api.INftInfoApi;
import com.binance.nft.assetservice.api.data.request.SelectUserNftByCollectionRequest;
import com.binance.nft.assetservice.api.data.vo.NftInfoDto;
import com.binance.nft.mystery.api.iface.INFTMysteryBoxQueryApi;
import com.binance.nft.tradeservice.api.launchpad.ILaunchpadConfigApi;
import com.binance.nft.tradeservice.api.launchpad.ILaunchpadOrderApi;
import com.binance.nft.tradeservice.enums.NftTypeEnum;
import com.binance.nft.tradeservice.request.launchpad.LaunchpadNftStakingRequest;
import com.binance.nft.tradeservice.request.launchpad.LaunchpadPurchaseRequest;
import com.binance.nft.tradeservice.request.launchpad.StakingStatusQueryRequest;
import com.binance.nft.tradeservice.response.launchpad.LaunchpadDetailResponse;
import com.binance.nft.tradeservice.response.launchpad.LaunchpadPurchaseResponse;
import com.binance.platform.mgs.annotations.UserOperation;
import com.binance.platform.mgs.base.helper.BaseHelper;
import com.binance.platform.mgs.base.helper.CrowdinHelper;
import com.binance.platform.mgs.base.vo.CommonRet;
import com.google.common.collect.Lists;
import io.swagger.annotations.Api;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.validation.Valid;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Api
@Slf4j
@RestController
@RequestMapping("/v1")
public class LaunchpadController {

    @Resource
    private ILaunchpadConfigApi launchpadConfigApi;
    @Resource
    private ILaunchpadOrderApi launchpadOrderApi;
    @Resource
    private BaseHelper baseHelper;
    @Resource
    private TradeCacheProxy tradeCacheProxy;
    @Resource
    private CrowdinHelper crowdinHelper;
    @Resource
    private INftInfoApi iNftInfoApi;
    @Resource
    private INFTMysteryBoxQueryApi inftMysteryBoxQueryApi;

    /**
     * launchpad详情
     * @return
     */
    @GetMapping("friendly/nft/nft-trade/launchpad-detail/{pageLink}")
    public CommonRet<LaunchpadDetailResponse> launchpadDetail(@PathVariable("pageLink") String pageLink) throws Exception {
        Long userId = baseHelper.getUserId();
        LaunchpadDetailResponse data  = null;
        if(Objects.isNull(userId)) {
            data = tradeCacheProxy.launchpadDetail(pageLink);
        } else {
            APIResponse<LaunchpadDetailResponse> response = launchpadConfigApi.detail(pageLink);
            baseHelper.checkResponse(response);
            data = response.getData();
        }

        data.setSubTitle(crowdinHelper.getMessageByKey(data.getSubTitle(), baseHelper.getLanguage()));
        data.setDescription(crowdinHelper.getMessageByKey(data.getDescription(), baseHelper.getLanguage()));
        data.setRules(crowdinHelper.getMessageByKey(data.getRules(), baseHelper.getLanguage()));
        return new CommonRet<>(data);
    }



    /**
     * 下单
     * @return
     */
    @PostMapping({"/private/nft/nft-trade/staking", "/private/nft/nft-trade/launchpad-purchase"})
    @UserOperation(eventName = "NFT_Launchpad_Purchase", name = "NFT_Launchpad_Purchase", sendToBigData = true, sendToDb = true,
            responseKeys = {"$.code","$.message","$.data", "$.errorMessage", "$.errorCode"},
            responseKeyDisplayNames = {"code","message","data","errorMessage","errorCode"})
    public CommonRet<LaunchpadPurchaseResponse> purchase(@Valid @RequestBody LaunchpadPurchaseRequest request) throws Exception {
        request.setUserId(baseHelper.getUserId());
        APIResponse<LaunchpadPurchaseResponse> response = launchpadOrderApi.purchase(APIRequest.instance(request));
        baseHelper.checkResponse(response);
        return new CommonRet<>(response.getData());
    }



    /**
     * 订单查询
     * @return
     */
    @UserOperation(eventName = "NFT_Launchpad_Status", name = "NFT_Launchpad_Status",
            responseKeys = {"$.code", "$.message", "$.errorMessage", "$.errorCode"},
            responseKeyDisplayNames = {"code", "message", "errorMessage", "errorCode"})
    @PostMapping("/private/nft/nft-trade/staking-query")
    public CommonRet<String> queryStatus(@Valid @RequestBody StakingStatusQueryRequest request) throws Exception {
        request.setUserId(baseHelper.getUserId());
        APIResponse<String> response = launchpadOrderApi.query(APIRequest.instance(request));
        baseHelper.checkResponse(response);
        return new CommonRet<>(response.getData());
    }


    /**
     * 查询CollectionID 对应的资产数据集合
     * @return
     */
    @PostMapping("/private/nft/nft-trade/staking-collection-query")
    public CommonRet<QueryAssetbyCollectionIdResponse> queryAssetbyCollectionId(@Valid @RequestBody QueryAssetbyCollectionIdRequest request) throws Exception {
        APIResponse<List<NftInfoDto>> response = iNftInfoApi.selectUserNftByCollection(APIRequest.instance(SelectUserNftByCollectionRequest.builder().collectionId(request.getCollectionId()).userId(baseHelper.getUserId()).build()));
        com.binance.nftcore.utils.lambda.check.BaseHelper.checkResponse(response);
        QueryAssetbyCollectionIdResponse collectionIdResponse = QueryAssetbyCollectionIdResponse.builder().build();
        if(CollectionUtils.isNotEmpty(response.getData())) {
           initNftCollection(response,collectionIdResponse);
        }
        return new CommonRet<>(collectionIdResponse);
    }

    private void initNftCollection(APIResponse<List<NftInfoDto>> response, QueryAssetbyCollectionIdResponse idResponse) {
        Byte nftType = response.getData().get(0).getNftType();
        idResponse.setNftType(nftType.intValue());
        if(NftTypeEnum.NORMAL.typeEquals(nftType.intValue())) {
            idResponse.setItems(response.getData().stream().map(item -> ItemCollectionVo.builder().url(item.getZippedUrl()).title(item.getNftTitle()).
                    contractAddress(item.getContractAddress()).tokenId(item.getTokenId())
                    .count(1).nftInfos(Lists.newArrayList(item)).build()).collect(Collectors.toList()));
            return ;
        }

        Map<String, List<NftInfoDto>> items = response.getData().stream().collect(Collectors.groupingBy(NftInfoDto::getNftTitle));
        List<ItemCollectionVo> collect = items.keySet().stream().map(item -> {
            NftInfoDto nftInfoDto = items.get(item).get(0);
            ItemCollectionVo build = ItemCollectionVo.builder().title(item).nftInfos(items.get(item)).count(items.get(item).size()).contractAddress(nftInfoDto.getContractAddress()).url(nftInfoDto.getZippedUrl()).build();
            return build;
        }).collect(Collectors.toList());
        idResponse.setItems(collect);
    }


    /**
     * nftStaking
     */
    @PostMapping("/private/nft/nft-trade/staking-nft")
    @UserOperation(eventName = "NFT_Launchpad_Staking", name = "NFT_Launchpad_Staking", sendToBigData = true, sendToDb = true,
            responseKeys = {"$.code","$.message","$.data", "$.errorMessage", "$.errorCode"},
            responseKeyDisplayNames = {"code","message","data","errorMessage","errorCode"})
    public CommonRet<Void> nftStaking(@Valid @RequestBody LaunchpadNftStakingRequest request) throws Exception {
        request.setUserId(baseHelper.getUserId());
        APIResponse<Void> response = launchpadOrderApi.nftStaking(APIRequest.instance(request));
        baseHelper.checkResponse(response);
        return new CommonRet<>();
    }

    /**
     * nftStaking
     */
    @UserOperation(eventName = "NFT_Launchpad_Untaking", name = "NFT_Launchpad_Untaking",
            responseKeys = {"$.code", "$.message", "$.errorMessage", "$.errorCode"},
            responseKeyDisplayNames = {"code", "message", "errorMessage", "errorCode"})
    @PostMapping("/private/nft/nft-trade/unstaking-nft")
    public CommonRet<Void> nftUnstaking(@Valid @RequestBody LaunchpadNftStakingRequest request) throws Exception {
        request.setUserId(baseHelper.getUserId());
        APIResponse<Void> response = launchpadOrderApi.nftUnstaking(APIRequest.instance(request));
        baseHelper.checkResponse(response);
        return new CommonRet<>();
    }
}
