package com.binance.mgs.nft.nftasset.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.binance.master.models.APIRequest;
import com.binance.master.models.APIResponse;
import com.binance.master.utils.CopyBeanUtils;
import com.binance.mgs.nft.nftasset.controller.helper.ActivityCR7Helper;
import com.binance.mgs.nft.nftasset.controller.helper.MysteryBoxHelper;
import com.binance.mgs.nft.nftasset.controller.helper.PojoConvertor;
import com.binance.mgs.nft.nftasset.controller.helper.ProductHelper;
import com.binance.mgs.nft.nftasset.vo.MysteryBoxSimpleDto;
import com.binance.mgs.nft.nftasset.vo.*;
import com.binance.nft.assetservice.api.INftAssetApi;
import com.binance.nft.assetservice.api.data.request.*;
import com.binance.nft.assetservice.api.data.response.OpenMysteryBoxResponse;
import com.binance.nft.assetservice.api.data.vo.*;
import com.binance.nft.assetservice.enums.FreezeReasonEnum;
import com.binance.nft.mystery.api.vo.MysteryBoxProductDetailVo;
import com.binance.nft.tradeservice.vo.NftProductInfoVo;
import com.binance.platform.mgs.annotations.UserOperation;
import com.binance.platform.mgs.base.helper.BaseHelper;
import com.binance.platform.mgs.base.helper.CrowdinHelper;
import com.binance.platform.mgs.base.vo.CommonRet;
import io.swagger.annotations.Api;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.math.NumberUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

@Api
@Slf4j
@RequestMapping("/v1/private/nft/nft-mystery")
@RestController
@RequiredArgsConstructor
public class NftMysteryBoxController {

    private final INftAssetApi nftAssetApi;
    private final MysteryBoxHelper mysteryBoxHelper;
    private final BaseHelper baseHelper;
    private final ProductHelper productHelper;
    private final PojoConvertor pojoConvertor;

    private final CrowdinHelper crowdinHelper;
    private final ActivityCR7Helper activityCR7Helper;

    @Value("${nft.mysterybox.open.limit:100}")
    private Integer OPEN_LIMIT;

    @PostMapping("/serial")
    CommonRet<MysteryBoxSerialRet> queryMysteryBoxSerial(@RequestBody @Valid MysteryBoxSerialArg request) {

        Long userId = baseHelper.getUserId();
        if (null == userId) {
            return new CommonRet<>();
        }

        MysteryBoxSerialRequest serialRequest = MysteryBoxSerialRequest.builder()
                .serialsNo(
                        Long.parseLong(request.getSerialsNo())
                ).build();
        serialRequest.setUserId(userId);
        APIResponse<MysteryBoxSerialVo> apiResponse = nftAssetApi.queryMysteryBoxSerial(APIRequest.instance(serialRequest));
        baseHelper.checkResponse(apiResponse);
        // 排除Long
        MysteryBoxSerialVo data = apiResponse.getData();
        MysteryBoxSerialRet mysteryBoxSerialRet = new MysteryBoxSerialRet();
        mysteryBoxSerialRet.setSerialUrl(data.getSerialUrl());
        mysteryBoxSerialRet.setSerialTitle(data.getSerialTitle());
        mysteryBoxSerialRet.setQuantity(data.getQuantity());
        mysteryBoxSerialRet.setSerialsNo(String.valueOf(data.getSerialsNo()));
        mysteryBoxSerialRet.setContractAddress(data.getContractAddress());
        mysteryBoxSerialRet.setMerchantAvatar(data.getMerchantAvatar());
        mysteryBoxSerialRet.setMerchantName(data.getMerchantName());

        return new CommonRet<>(mysteryBoxSerialRet);
    }

    @PostMapping("/open")
    @UserOperation(eventName = "openBox", name = "openBox", sendToBigData = true, sendToDb = true,
            responseKeys = {"$.code","$.message","$.data", "$.errorMessage", "$.errorCode"},
            responseKeyDisplayNames = {"code","message","data","errorMessage","errorCode"})
    CommonRet<OpenMysteryBoxResponse> openMysteryBox(@RequestBody @Valid OpenMysteryBoxArg request) {

        Long userId = baseHelper.getUserId();
        if (Objects.isNull(userId)
                || Objects.isNull(request)) {
            return new CommonRet<>();
        }

        if (request.getNumber() >= OPEN_LIMIT){
            return new CommonRet<>();
        }

        if (activityCR7Helper.checkCr7Collection(NumberUtils.toLong(request.getSerialsNo()))){
            return new CommonRet<>();
        }

        OpenMysteryBoxRequest openMysteryBoxRequest = OpenMysteryBoxRequest.builder()
                .number(request.getNumber())
                .serialsNo(Long.parseLong(request.getSerialsNo()))
                .nftIds(request.getNftIds())
                .build();
        openMysteryBoxRequest.setUserId(userId);
        APIResponse<OpenMysteryBoxResponse> apiResponse = nftAssetApi.openMysteryBox(APIRequest.instance(openMysteryBoxRequest));

        baseHelper.checkResponse(apiResponse);
        return new CommonRet<>(apiResponse.getData());
    }

    @UserOperation(eventName = "NFT_Mystery_Open", name = "NFT_Mystery_Open",
            responseKeys = {"$.code", "$.message", "$.errorMessage", "$.errorCode"},
            responseKeyDisplayNames = {"code", "message", "errorMessage", "errorCode"})
    @PostMapping("/open-status")
    CommonRet<MysteryBoxBatchVo> queryMysteryBoxStatus(@RequestBody @Valid MysteryBoxOpenProcessingArg request) {

        Long userId = baseHelper.getUserId();
        if (null == userId) {
            return new CommonRet<>();
        }

        OpenMysteryBoxStatusRequest batchProcessingRequest = OpenMysteryBoxStatusRequest.builder()
                .batchOpenId(Long.parseLong(request.getBatchOpenId()))
                .build();
        batchProcessingRequest.setUserId(userId);
        APIResponse<MysteryBoxBatchVo> apiResponse = nftAssetApi.queryMysteryBoxStatus(APIRequest.instance(batchProcessingRequest));

        baseHelper.checkResponse(apiResponse);
        return new CommonRet<>(apiResponse.getData());
    }

    @PostMapping("/item")
    CommonRet<MysteryBoxItemVo> queryMysteryBoxItem(@RequestBody @Valid MysteryBoxItemArg request) {
        Long userId = baseHelper.getUserId();
        if (null == userId) {
            return new CommonRet<>();
        }

        MysteryBoxItemRequest itemRequest = MysteryBoxItemRequest.builder()
                .itemId(Long.parseLong(request.getItemId()))
                .build();
        itemRequest.setUserId(userId);
        APIResponse<MysteryBoxItemVo> apiResponse = nftAssetApi.queryMysteryBoxItem(APIRequest.instance(itemRequest));

        baseHelper.checkResponse(apiResponse);
        Optional.ofNullable(apiResponse.getData()).ifPresent(i -> i.setMerchantId(null));
        return new CommonRet<>(apiResponse.getData());
    }

    @PostMapping("/collection/page")
    CommonRet<Page<MysteryBoxSimpleRet>> fetchMysteryBoxPageable(@RequestBody @Valid MysteryBoxPageArg request) {
        Long userId = baseHelper.getUserId();
        if (null == userId) {
            return new CommonRet<>();
        }

        MysteryBoxPageRequest pageRequest = CopyBeanUtils.fastCopy(request, MysteryBoxPageRequest.class);
        pageRequest.setUserId(userId);
        APIResponse<Page<MysteryBoxSimpleVo>> apiResponse = nftAssetApi.fetchMysteryBoxPageable(APIRequest.instance(pageRequest));

        baseHelper.checkResponse(apiResponse);
        Page<MysteryBoxSimpleVo> pageData = apiResponse.getData();
        if (CollectionUtils.isEmpty(pageData.getRecords())) {
            return new CommonRet<>();
        }

        List<MysteryBoxSimpleRet> list = new ArrayList<>(pageData.getRecords().size());

        pageData.getRecords().forEach(x -> {
            MysteryBoxSimpleRet ret = new MysteryBoxSimpleRet();
            ret.setSerialsName(x.getSerialsName());
            ret.setNftType(x.getNftType());
            ret.setQuantity(
                    Integer.parseInt(
                            Optional.ofNullable(x.getQuantity()).orElse("0")
                    ));
            ret.setSerialsNo(x.getSerialsNo());
            ret.setZippedUrl(x.getZippedUrl());
            ret.setItemSimpleVoList(x.getItemSimpleVoList());
            ret.setMysteryboxCount(x.getMysteryboxCount());
            ret.setAssetStatus(x.getAssetStatus());
            ret.setNetwork(x.getNetwork());
            list.add(ret);
        });

        Page<MysteryBoxSimpleRet> retPage = new Page<>();
        retPage.setCurrent(pageData.getCurrent());
        retPage.setSize(pageData.getSize());
        retPage.setTotal(pageData.getTotal());
        retPage.setPages(pageData.getPages());
        retPage.setRecords(list);
        return new CommonRet<>(retPage);
    }

    @PostMapping("/collection/totalpage")
    CommonRet<MysteryBoxSimpleTotalVo> fetchMysteryBoxPageableV2(@RequestBody @Valid MysteryBoxPageArg request) {
        Long userId = baseHelper.getUserId();
        if (null == userId) {
            return new CommonRet<>();
        }

        MysteryBoxPageRequest pageRequest = CopyBeanUtils.fastCopy(request, MysteryBoxPageRequest.class);
        pageRequest.setUserId(userId);
        APIResponse<MysteryBoxSimpleVoTotal> apiResponse = nftAssetApi.fetchMysteryBoxPageableV2(APIRequest.instance(pageRequest));

        baseHelper.checkResponse(apiResponse);
        final MysteryBoxSimpleVoTotal data = apiResponse.getData();
        if (CollectionUtils.isEmpty(data.getMysteryBoxSimpleVoPage().getRecords())){
            return new CommonRet<>();
        }

        final MysteryBoxSimpleTotalVo mysteryBoxSimpleTotalVo = pojoConvertor.copyMysteryBoxTotalPage(data);
        for (MysteryBoxSimpleDto record : mysteryBoxSimpleTotalVo.getMysteryBoxSimpleVoPage().getRecords()) {
            MysteryBoxProductDetailVo mysteryBoxDetailForMeta = mysteryBoxHelper.queryMysteryBoxDetailForMeta(Long.parseLong(record.getSerialsNo()));
            if (Objects.isNull(mysteryBoxDetailForMeta)
                    || Objects.isNull(mysteryBoxDetailForMeta.getDuration())
                    || Objects.isNull(mysteryBoxDetailForMeta.getSecondMarketSellingDelay())){
                log.warn("mysteryBoxDetailForMeta :: {}", Objects.isNull(mysteryBoxDetailForMeta) ? null : mysteryBoxDetailForMeta.getSerialsNo());
                continue;
            }
            record.setSecondMarketSellingDelay(mysteryBoxDetailForMeta.getSecondMarketSellingDelay());
            record.setDuration(Long.valueOf(mysteryBoxDetailForMeta.getDuration()));
        }

        return new CommonRet<>(mysteryBoxSimpleTotalVo);
    }

    @PostMapping("/collection/total-serials")
    CommonRet<MysteryBoxSimpleSerialsTotalVo> fetchMysteryBoxSerialsPageable(@RequestBody @Valid MysteryBoxSerialsPageArg request) {
        Long userId = baseHelper.getUserId();
        if (null == userId) {
            return new CommonRet<>();
        }

        MysteryBoxPageRequest pageRequest = CopyBeanUtils.fastCopy(request, MysteryBoxPageRequest.class);
        pageRequest.setUserId(userId);
        pageRequest.setPage(0);
        pageRequest.setPageSize(0);
        final APIResponse<MysteryBoxSerialsSimpleVoTotal> apiResponse = nftAssetApi.fetchMysteryBoxSerialsPageable(APIRequest.instance(pageRequest));

        baseHelper.checkResponse(apiResponse);

        final MysteryBoxSerialsSimpleVoTotal data = apiResponse.getData();
        if (CollectionUtils.isEmpty(data.getMysteryBoxSerialsSimpleVo())){
            return new CommonRet<>();
        }

        mysteryBoxHelper.getMysteryMetaBatch(data);
        return new CommonRet<>(pojoConvertor.copyMysteryBoxSerialsTotalPage(data));
    }

    @PostMapping("/collection/total-items")
    CommonRet<List<MysteryBoxSimpleItemVo>> fetchMysteryBoxItemsPageable(@RequestBody @Valid MysteryBoxItemPageArg request) {
        Long userId = baseHelper.getUserId();
        if (null == userId) {
            return new CommonRet<>();
        }

        MysteryBoxItemPageRequest pageRequest = CopyBeanUtils.fastCopy(request, MysteryBoxItemPageRequest.class);
        pageRequest.setUserId(userId);
        pageRequest.setPage(0);
        pageRequest.setPageSize(0);
        final APIResponse<List<MysteryBoxItemSimpleVo>> apiResponse = nftAssetApi.fetchMysteryBoxItemPageable(APIRequest.instance(pageRequest));

        baseHelper.checkResponse(apiResponse);
        final List<MysteryBoxItemSimpleVo> data = apiResponse.getData();
        if (CollectionUtils.isEmpty(data)){
            return new CommonRet<>();
        }

        final List<MysteryBoxSimpleItemVo> resultVo = pojoConvertor.copyMysteryBoxItemTotalPage(data);
        if (CollectionUtils.isNotEmpty(resultVo)) {
            for (MysteryBoxSimpleItemVo record : resultVo) {
                if(StringUtils.isNotBlank(record.getFreezeReasonId())){
                    String key = "nft-freeze-reason-00" + record.getFreezeReasonId();;
                    String message = crowdinHelper.getMessageByKey(key, baseHelper.getLanguage());
                    message = StringUtils.equals(message, key) ?
                            FreezeReasonEnum.findReasonById(
                                    Integer.valueOf(record.getFreezeReasonId())).getDescription()
                            : message;
                    record.setFreezeReason(message);
                }
            }
        }
        MysteryBoxProductDetailVo mysteryBoxDetailForMeta = mysteryBoxHelper.queryMysteryBoxDetailForMeta(request.getSerialsNo());
        if (Objects.isNull(mysteryBoxDetailForMeta)
                || Objects.isNull(mysteryBoxDetailForMeta.getDuration())
                || Objects.isNull(mysteryBoxDetailForMeta.getSecondMarketSellingDelay())){
            return new CommonRet<>(resultVo);
        }
        for (MysteryBoxSimpleItemVo record : resultVo) {
            record.setSecondMarketSellingDelay(mysteryBoxDetailForMeta.getSecondMarketSellingDelay());
            record.setDuration(Long.valueOf(mysteryBoxDetailForMeta.getDuration()));
        }

        return new CommonRet<>(resultVo);
    }

    @PostMapping("/on-sale/page")
    CommonRet<Page<MysteryBoxProductSimpleRet>> fetchOnSaleMysteryBoxPageable(@RequestBody @Valid MysteryBoxPageArg request) {

        Long userId = baseHelper.getUserId();
        if (null == userId) {
            return new CommonRet<>();
        }

        MysteryBoxPageRequest pageRequest = MysteryBoxPageRequest.builder()
                .userId(userId)
                .openStatus(request.getOpenStatus())
                .page(request.getPage())
                .pageSize(request.getPageSize())
                .build();
        APIResponse<Page<MysteryBoxOnSaleSimpleVo>> apiResponse = nftAssetApi.fetchMysteryBoxOnsalePageable(APIRequest.instance(pageRequest));
        baseHelper.checkResponse(apiResponse);

        Page<MysteryBoxOnSaleSimpleVo> pageData = apiResponse.getData();
        if (CollectionUtils.isEmpty(pageData.getRecords())) {
            return new CommonRet<>();
        }
        AtomicReference<Boolean> modifyTotalFlag = new AtomicReference<>(Boolean.FALSE);

        final List<NftProductInfoVo> data = productHelper.getProductInfoMysteryBox(pageData, modifyTotalFlag);
        final List<MysteryBoxProductSimpleRet> list = productHelper.convertData(data, pageData);

        Page<MysteryBoxProductSimpleRet> retPage = new Page<>();
        retPage.setCurrent(pageData.getCurrent());
        retPage.setSize(pageData.getSize());
        retPage.setTotal(modifyTotalFlag.get() ? pageData.getTotal() - NumberUtils.LONG_ONE : pageData.getTotal());
        retPage.setPages(pageData.getPages());
        retPage.setRecords(list);
        return new CommonRet<>(retPage);
    }

}
