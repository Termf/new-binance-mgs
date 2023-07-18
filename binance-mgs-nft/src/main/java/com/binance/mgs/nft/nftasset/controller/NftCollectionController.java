package com.binance.mgs.nft.nftasset.controller;

import com.binance.master.error.GeneralCode;
import com.binance.master.models.APIRequest;
import com.binance.master.models.APIResponse;
import com.binance.mgs.nft.nftasset.controller.helper.FreezeReasonHelper;
import com.binance.mgs.nft.nftasset.controller.helper.NftAssetHelper;
import com.binance.mgs.nft.nftasset.controller.helper.PojoConvertor;
import com.binance.mgs.nft.nftasset.controller.helper.UserHelper;
import com.binance.mgs.nft.nftasset.vo.*;
import com.binance.mgs.nft.reconciliation.helper.ReconciliationHelper;
import com.binance.nft.assetservice.api.data.request.function.CollectionNftRenamingRequest;
import com.binance.nft.assetservice.api.data.response.CreateLayerResp;
import com.binance.nft.assetservice.api.data.vo.function.NftLayerConfigVo;
import com.binance.nft.assetservice.api.function.ILayerInfoApi;
import com.binance.nft.assetservice.api.validator.CreationActionGroup;
import com.binance.nft.assetservice.constant.NftAssetErrorCode;
import com.binance.nft.assetservice.enums.NetworkEnum;
import com.binance.nft.assetservice.enums.NftSourceEnum;
import com.binance.nft.reconciliaction.constant.FeeTypeEnum;
import com.binance.platform.mgs.annotations.UserOperation;
import com.binance.platform.mgs.base.helper.BaseHelper;
import com.binance.platform.mgs.base.vo.CommonRet;
import com.google.common.collect.ImmutableMap;
import io.swagger.annotations.Api;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.MapUtils;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Objects;

@Api
@Slf4j
@RequestMapping("/v1")
@RestController
@RequiredArgsConstructor
public class NftCollectionController {

    private final BaseHelper baseHelper;
    private final ILayerInfoApi layerInfoApi;
    private final PojoConvertor pojoConvertor;
    private final ReconciliationHelper reconciliationHelper;

    private final UserHelper userHelper;
    private final FreezeReasonHelper freezeReasonHelper;
    private final NftAssetHelper nftAssetHelper;

    @GetMapping("/private/nft/nft-asset/user-shadow-collection/nft-page")
    public CommonRet<CollectionNamingVo> fetchUserNamingCollectionPage(@RequestParam("page") int page, @RequestParam("size")int size,
                                                                           @RequestParam(name = "collectionId", required = false) Long collectionId){

        final Long userId = baseHelper.getUserId();
        if (Objects.isNull(userId)){
            return new CommonRet<>();
        }

        final APIResponse<com.binance.nft.assetservice.api.data.vo.function.CollectionNamingVo> apiResponse =
                layerInfoApi.fetchUserNamingCollectionPage(userId, page, size, collectionId);
        baseHelper.checkResponse(apiResponse);

        return new CommonRet<>(pojoConvertor.copyCollectionNamingVo2Mgs(apiResponse.getData()));
    }

    @GetMapping("/private/nft/nft-asset/user-collection-detail/{collection-id}")
    public CommonRet<CollectionCreateArg> fetchUserCollection(@PathVariable("collection-id") Long collectionId){

        final Long userId = baseHelper.getUserId();
        if (Objects.isNull(userId) || Objects.isNull(collectionId)){
            return new CommonRet<>();
        }

        final APIResponse<NftLayerConfigVo> apiResponse = layerInfoApi.fetchUserCollectionByCollectionId(userId, collectionId);
        baseHelper.checkResponse(apiResponse);

        return new CommonRet<>(pojoConvertor.copyCollectionCreateVo2Arg(apiResponse.getData()));
    }

    @UserOperation(eventName = "NFT_Collection_Create", name = "NFT_Collection_Create",
            responseKeys = {"$.code", "$.message", "$.errorMessage", "$.errorCode"},
            responseKeyDisplayNames = {"code", "message", "errorMessage", "errorCode"})
    @PostMapping("/private/nft/nft-asset/user-collection-create")
    public CommonRet<Object> createUserCollection(@Validated(value = CreationActionGroup.class) @RequestBody CollectionCreateArg arg){

        final Long userId = baseHelper.getUserId();
        if (Objects.isNull(userId)){
            return new CommonRet<>();
        }

        nftAssetHelper.preMintCheckForCollection(userId, null);

        Integer feeCode = null;
        if (NetworkEnum.BSC.getCode().equals(arg.getNetworkType())){
            feeCode = FeeTypeEnum.CREATING_CONTRACT_BSC.getCode();
        }else if (NetworkEnum.ERC.getCode().equals(arg.getNetworkType())){
            feeCode = FeeTypeEnum.CREATING_CONTRACT_ERC.getCode();
        }

        final ReconciliationHelper.ReconciliationDto reconciliationDto = reconciliationHelper.getReconciliationDtoByFeeType(feeCode);

        arg.setUserId(userId);
        arg.setAsset(reconciliationDto.getCurrency());
        arg.setContractOnChainFee(reconciliationDto.getAmount());
        arg.setSource(Integer.valueOf(NftSourceEnum.BINANCE_NFT.getCode()));
        arg.setNeedNewContract(Boolean.TRUE);
        final APIResponse<Boolean> apiResponse = layerInfoApi.createUserLayer(APIRequest.instance(pojoConvertor.copyCollectionCreateArg2Vo(arg)));
        if (Objects.nonNull(apiResponse.getCode())
                && apiResponse.getCode().equalsIgnoreCase(NftAssetErrorCode.CONTENT_ILLEGAL.getCode())) {
            CommonRet<Object> result = new CommonRet<>();
            result.setCode(GeneralCode.SYS_VALID.getCode());
            result.setMessage(GeneralCode.SYS_VALID.toString());
            if (Objects.nonNull(apiResponse.getParams()) && apiResponse.getParams().length > 0) {
                result.setData(ImmutableMap.of("sensitiveFields", apiResponse.getParams()));
            }
            return result;
        }
        baseHelper.checkResponse(apiResponse);

        return new CommonRet<>(apiResponse.getData());
    }

    @UserOperation(eventName = "NFT_Collection_Create", name = "NFT_Collection_Create",
            responseKeys = {"$.code", "$.message", "$.errorMessage", "$.errorCode"},
            responseKeyDisplayNames = {"code", "message", "errorMessage", "errorCode"})
    @PostMapping("/private/nft/nft-asset/user-collection-create/v2")
    public CommonRet<Object> createUserCollectionV2(@Validated(value = CreationActionGroup.class) @RequestBody CollectionCreateArg arg){

        final Long userId = baseHelper.getUserId();
        if (Objects.isNull(userId)){
            return new CommonRet<>();
        }

        nftAssetHelper.preMintCheckForCollection(userId, null);

        Integer feeCode = null;
        if (NetworkEnum.BSC.getCode().equals(arg.getNetworkType())){
            feeCode = FeeTypeEnum.CREATING_CONTRACT_BSC.getCode();
        }else if (NetworkEnum.ERC.getCode().equals(arg.getNetworkType())){
            feeCode = FeeTypeEnum.CREATING_CONTRACT_ERC.getCode();
        }
        final ReconciliationHelper.ReconciliationDto reconciliationDto = reconciliationHelper.getReconciliationDtoByFeeType(feeCode);
        arg.setUserId(userId);
        arg.setAsset(reconciliationDto.getCurrency());
        arg.setContractOnChainFee(reconciliationDto.getAmount());
        arg.setSource(Integer.valueOf(NftSourceEnum.BINANCE_NFT.getCode()));
        arg.setNeedNewContract(Boolean.TRUE);
        APIResponse<CreateLayerResp> apiResponse = layerInfoApi.createUserLayerV2(APIRequest.instance(pojoConvertor.copyCollectionCreateArg2Vo(arg)));
        baseHelper.checkResponse(apiResponse);
        CreateLayerResp data = apiResponse.getData();
        if(data != null) {
            if (!data.isSuccess()) {
                CommonRet<Object> result = new CommonRet<>();
                result.setCode(GeneralCode.SYS_VALID.getCode());
                result.setMessage(GeneralCode.SYS_VALID.toString());
                Map<String, Object> params = data.getParams();
                if (MapUtils.isNotEmpty(params)) {
                    freezeReasonHelper.enrichMessage(params);
                    result.setData(params);
                }
                return result;
            }
        }
        return new CommonRet<>(true);
    }

    @UserOperation(eventName = "NFT_Collection_Create", name = "NFT_Collection_Create",
            responseKeys = {"$.code", "$.message", "$.errorMessage", "$.errorCode"},
            responseKeyDisplayNames = {"code", "message", "errorMessage", "errorCode"})
    @PostMapping("/private/nft/nft-asset/user-collection-edit")
    public CommonRet<Object> editUserCollection(@RequestBody CollectionCreateArg arg){

        final Long userId = baseHelper.getUserId();
        if (Objects.isNull(userId)){
            return new CommonRet<>();
        }

        arg.setUserId(userId);
        arg.setSource(Integer.valueOf(NftSourceEnum.BINANCE_NFT.getCode()));
        final APIResponse<Boolean> apiResponse = layerInfoApi.editUserLayer(APIRequest.instance(pojoConvertor.copyCollectionCreateArg2Vo(arg)));
        if (Objects.nonNull(apiResponse.getCode())
                && apiResponse.getCode().equalsIgnoreCase(NftAssetErrorCode.CONTENT_ILLEGAL.getCode())) {
            CommonRet<Object> result = new CommonRet<>();
            result.setCode(GeneralCode.SYS_VALID.getCode());
            result.setMessage(GeneralCode.SYS_VALID.toString());
            if (Objects.nonNull(apiResponse.getParams()) && apiResponse.getParams().length > 0) {
                result.setData(ImmutableMap.of("sensitiveFields", apiResponse.getParams()));
            }
            return result;
        }
        baseHelper.checkResponse(apiResponse);

        return new CommonRet<>(apiResponse.getData());
    }

    @UserOperation(eventName = "NFT_Collection_Create", name = "NFT_Collection_Create",
            responseKeys = {"$.code", "$.message", "$.errorMessage", "$.errorCode"},
            responseKeyDisplayNames = {"code", "message", "errorMessage", "errorCode"})
    @PostMapping("/private/nft/nft-asset/user-collection-edit/v2")
    public CommonRet<Object> editUserCollectionV2(@RequestBody CollectionCreateArg arg){

        final Long userId = baseHelper.getUserId();
        if (Objects.isNull(userId)){
            return new CommonRet<>();
        }

        arg.setUserId(userId);
        arg.setSource(Integer.valueOf(NftSourceEnum.BINANCE_NFT.getCode()));
        APIResponse<CreateLayerResp> apiResponse = layerInfoApi.editUserLayerV2(APIRequest.instance(pojoConvertor.copyCollectionCreateArg2Vo(arg)));

        baseHelper.checkResponse(apiResponse);
        CreateLayerResp data = apiResponse.getData();
        if(data != null) {
            if (!data.isSuccess()) {
                CommonRet<Object> result = new CommonRet<>();
                result.setCode(GeneralCode.SYS_VALID.getCode());
                result.setMessage(GeneralCode.SYS_VALID.toString());
                Map<String, Object> params = data.getParams();
                if (MapUtils.isNotEmpty(params)) {
                    freezeReasonHelper.enrichMessage(params);
                    result.setData(params);
                }
                return result;
            }
        }
        return new CommonRet<>(true);
    }


    @UserOperation(eventName = "NFT_Collection_Create", name = "NFT_Collection_Create",
            responseKeys = {"$.code", "$.message", "$.errorMessage", "$.errorCode"},
            responseKeyDisplayNames = {"code", "message", "errorMessage", "errorCode"})
    @PostMapping("/private/nft/nft-asset/user-shadow-collection/creation")
    public CommonRet<Boolean> createShadowCollection(@RequestBody @Validated ShadowCollectionConfigArg arg){

        final Long userId = baseHelper.getUserId();
        if (Objects.isNull(userId)
                || !userHelper.checkUserWhiteList(userId).isMintFlag()){
            return new CommonRet<>();
        }

        final CollectionNftRenamingRequest renamingRequest = pojoConvertor.copyNftRenamingArg2Request(arg);
        renamingRequest.setUserId(userId);

        final APIResponse<Boolean> apiResponse = layerInfoApi.createShadowCollection(APIRequest.instance(renamingRequest));
        baseHelper.checkResponse(apiResponse);

        return new CommonRet<>(apiResponse.getData());
    }

    @UserOperation(eventName = "NFT_Collection_Create", name = "NFT_Collection_Create",
            responseKeys = {"$.code", "$.message", "$.errorMessage", "$.errorCode"},
            responseKeyDisplayNames = {"code", "message", "errorMessage", "errorCode"})
    @PostMapping("/private/nft/nft-asset/user-shadow-collection/nft-transfer")
    public CommonRet<Boolean> transferShadowCollectionNft(@RequestBody ShadowCollectionNftTransformArg arg){

        final Long userId = baseHelper.getUserId();
        if (Objects.isNull(userId)){
            return new CommonRet<>();
        }

        final CollectionNftRenamingRequest renamingRequest = pojoConvertor.copyNftTransformArg2Request(arg);
        renamingRequest.setUserId(userId);

        final APIResponse<Boolean> apiResponse = layerInfoApi.renamingCollection(APIRequest.instance(renamingRequest));
        baseHelper.checkResponse(apiResponse);

        return new CommonRet<>(apiResponse.getData());
    }

    @GetMapping("/private/nft/nft-asset/user-shadow-collection/name-list")
    public CommonRet<List<CollectionNftDto>> fetchUserShadowCollectionNameList(){

        final Long userId = baseHelper.getUserId();
        if (Objects.isNull(userId)){
            return new CommonRet<>();
        }

        final APIResponse<List<com.binance.nft.assetservice.api.data.vo.function.CollectionNftDto>> apiResponse = layerInfoApi.fetchUserCollectionWithShadow(userId);
        baseHelper.checkResponse(apiResponse);

        return new CommonRet<>(pojoConvertor.copyCollectionNft2MgsList(apiResponse.getData()));
    }

}
