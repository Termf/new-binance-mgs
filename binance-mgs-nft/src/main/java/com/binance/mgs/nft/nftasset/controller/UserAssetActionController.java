package com.binance.mgs.nft.nftasset.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.binance.master.models.APIRequest;
import com.binance.master.models.APIResponse;
import com.binance.master.utils.JsonUtils;
import com.binance.mgs.nft.inbox.HistoryType;
import com.binance.mgs.nft.inbox.NftInboxHelper;
import com.binance.mgs.nft.nftasset.vo.LayerOrderItemVo;
import com.binance.mgs.nft.nftasset.vo.MintOrderItemVo;
import com.binance.nft.assetservice.api.IUserActionOrderApi;
import com.binance.nft.assetservice.api.data.dto.OrderErrorSubType;
import com.binance.nft.assetservice.api.data.request.mintmanager.MintOrderReq;
import com.binance.nft.assetservice.api.data.response.mintmanager.LayerOrderItemResp;
import com.binance.nft.assetservice.api.data.response.mintmanager.MintOrderItemResp;
import com.binance.nft.assetservice.enums.CategoryEnum;
import com.binance.nft.assetservice.enums.HiveResultConvert;
import com.binance.nft.notificationservice.api.data.bo.BizIdModel;
import com.binance.nft.tradeservice.constant.Constants;
import com.binance.nft.tradeservice.vo.CategoryVo;
import com.binance.platform.mgs.base.helper.BaseHelper;
import com.binance.platform.mgs.base.helper.CrowdinHelper;
import com.binance.platform.mgs.base.vo.CommonRet;
import io.swagger.annotations.Api;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.web.bind.annotation.*;

import javax.validation.constraints.NotNull;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Api
@Slf4j
@RestController
@RequestMapping("/v1/private/nft")
@RequiredArgsConstructor
public class UserAssetActionController {

    private final IUserActionOrderApi userActionOrderApi;

    private final BaseHelper baseHelper;

    private final CrowdinHelper crowdinHelper;

    private final NftInboxHelper nftInboxHelper;

    @GetMapping(value = "/user/order/mint/list")
    public CommonRet<Page<MintOrderItemVo>> mintOrderList(@NotNull @RequestParam int page,
                                                          @NotNull @RequestParam int pageSize,
                                                          @RequestParam(value = "status",required = false) Integer status,
                                                          @RequestParam(value = "selectedStartTime",required = false) Long startTime,
                                                          @RequestParam(value = "selectedEndTime",required = false) Long endTime) {

        Long userId = baseHelper.getUserId();
        if (null == userId) {
            return new CommonRet<>();
        }

        MintOrderReq mintOrderReq = MintOrderReq.builder()
                .pageSize(pageSize)
                .userId(userId)
                .current(page)
                .status(status)
                .build();

        if (startTime != null) {
            mintOrderReq.setStartTime(new Date(startTime));
        }
        if (endTime != null) {
            mintOrderReq.setEndTime(new Date(endTime));
        }
        APIResponse<Page<MintOrderItemResp>> response = userActionOrderApi.
                findMintOrderItemByCondition(APIRequest.instance(mintOrderReq));

        baseHelper.checkResponse(response);

        response.getData().getRecords().forEach(item -> {
            List<OrderErrorSubType> orderErrorSubType = updateText(item.getError());
            item.setError(orderErrorSubType);
        });
        List<BizIdModel> bizIds = Arrays.asList(new BizIdModel(HistoryType.CREATE.name(),response.getData().getRecords().stream().map(m->m.getMintOrderId()).collect(Collectors.toList())));
        Page<MintOrderItemVo> retResult = nftInboxHelper.pageResultWithFlag(response.getData(), MintOrderItemVo.class,
                MintOrderItemVo::getMintOrderId,bizIds , MintOrderItemVo::setUnreadFlag);
        return new CommonRet<>(retResult);
    }

    @GetMapping(value = "/user/order/layer/list")
    public CommonRet<Page<LayerOrderItemVo>> layerOrderList(@NotNull @RequestParam int page,
                                                              @NotNull @RequestParam int pageSize,
                                                              @RequestParam(value = "status",required = false) Integer status,
                                                              @RequestParam(value = "selectedStartTime",required = false) Long startTime,
                                                              @RequestParam(value = "selectedEndTime",required = false) Long endTime) {

        Long userId = baseHelper.getUserId();
        if (null == userId) {
            return new CommonRet<>();
        }

        MintOrderReq mintOrderReq = MintOrderReq.builder()
                .pageSize(pageSize)
                .userId(userId)
                .current(page)
                .status(status)
                .build();

        if (startTime != null) {
            mintOrderReq.setStartTime(new Date(startTime));
        }
        if (endTime != null) {
            mintOrderReq.setEndTime(new Date(endTime));
        }

        APIResponse<Page<LayerOrderItemResp>> response = userActionOrderApi.findLayerOrderItemByCondition(APIRequest.instance(mintOrderReq));

        baseHelper.checkResponse(response);
        enrichTextList(response.getData().getRecords());
        response.getData().getRecords().forEach(item -> {
            List<OrderErrorSubType> orderErrorSubType = updateText(item.getError());
            item.setError(orderErrorSubType);
        });
        // add history unread flag
        List<BizIdModel> bizIdModels = Arrays.asList(new BizIdModel(HistoryType.LAYER.name(), response.getData().getRecords().stream().map(l->l.getLayerId()).collect(Collectors.toList())));
        Page<LayerOrderItemVo> retResult = nftInboxHelper.pageResultWithFlag(response.getData(), LayerOrderItemVo.class,
                LayerOrderItemVo::getLayerId,bizIdModels , LayerOrderItemVo::setUnreadFlag);
        return new CommonRet<>(retResult);
    }

    private void enrichTextList(List<LayerOrderItemResp> itemRespList) {

        if(CollectionUtils.isEmpty(itemRespList)){
            return;
        }
        String json = crowdinHelper.getMessageByKey(Constants.TRADE_MARKET_CATEGORYS_KEY, baseHelper.getLanguage());
        Map<Integer, CategoryVo> categoryVoMap = new HashMap<>();
        if(!Objects.equals(json, Constants.TRADE_MARKET_CATEGORYS_KEY)){
            categoryVoMap = JsonUtils.toObjList(json, CategoryVo.class)
                    .stream().collect(Collectors.toMap(CategoryVo::getCode, Function.identity(), (v1, v2) -> v1));
        }
        if (MapUtils.isNotEmpty(categoryVoMap)) {
            for (LayerOrderItemResp item : itemRespList) {
                String category = item.getCategory();
                if(StringUtils.isNotBlank(category)){
                    CategoryEnum cate = CategoryEnum.getByDesc(category);
                    if(cate != null && categoryVoMap.get(cate.getCode()) != null) {
                        item.setCategory(categoryVoMap.get(cate.getCode()).getName());
                    }
                }
            }
        }
    }

    private List<OrderErrorSubType> updateText(List<OrderErrorSubType> error){

        if (CollectionUtils.isEmpty(error)){
            return Collections.emptyList();
        }
        error.forEach(item -> {
            if(StringUtils.isNotBlank(item.getSource())){
                HiveResultConvert convert = HiveResultConvert.findByClazz(item.getSource());
                if(convert != null) {
                    String message = crowdinHelper.getMessageByKey(convert.getCode(), baseHelper.getLanguage());
                    if(!StringUtils.equals(message, convert.getCode())){
                        item.setSource(message);
                    }else{
                        item.setSource(convert.getDefaultText());
                    }
                }
            }
            if(StringUtils.isNotBlank(item.getCheckType())){
                String checkType = item.getCheckType();
                if(checkType.equals("logo")){
                    String message = crowdinHelper.getMessageByKey(HiveResultConvert.selectLogo.getCode(), baseHelper.getLanguage());
                    if(StringUtils.equals(message, HiveResultConvert.selectLogo.getCode())){
                        item.setValue(HiveResultConvert.selectLogo.getDefaultText() + "'" + item.getValue() + "'");
                    }else{
                        item.setValue(message + "'" + item.getValue() + "'");
                    }
                }else{
                    HiveResultConvert convert = HiveResultConvert.findByClazz(item.getValue());
                    if(convert != null){
                        String message = crowdinHelper.getMessageByKey(convert.getCode(), baseHelper.getLanguage());
                        if(!StringUtils.equals(message, convert.getCode())){
                            item.setValue(message);
                        }else{
                            item.setValue(convert.getDefaultText());
                        }
                    }
                }
            }
        });
        return error;
    }
}
