package com.binance.mgs.nft.fantoken.controller;

import com.binance.master.models.APIRequest;
import com.binance.master.models.APIResponse;
import com.binance.platform.mgs.base.helper.BaseHelper;
import com.binance.platform.mgs.base.vo.CommonRet;
import com.binance.mgs.nft.mysterybox.helper.MysteryBoxI18nHelper;
import com.binance.mgs.nft.mysterybox.vo.MysteryBoxProductDetailIncludeAssetVo;
import com.binance.nft.mystery.api.external.iface.NFTMysteryBoxExternalApi;
import com.binance.nft.mystery.api.external.vo.ExternalMysteryBatchQueryByIdRequest;
import com.binance.nft.mystery.api.external.vo.ExternalMysteryBoxProductDetailVo;
import com.binance.nft.mystery.api.external.vo.ExternalMysteryBoxProductSimpleVo;
import com.binance.nft.mystery.api.external.vo.ExternalQueryMysteryBoxDetailRequest;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@RequestMapping("/v1/public/nft/fantoken")
@RestController
@RequiredArgsConstructor
public class FanTokenMysteryController {

    private final BaseHelper baseHelper;

    private final NFTMysteryBoxExternalApi mysteryBoxExternalApi;

    private final MysteryBoxI18nHelper mysteryBoxI18nHelper;

    @SneakyThrows
    @GetMapping("/mystery-box/detail")
    public CommonRet<MysteryBoxProductDetailIncludeAssetVo> queryMysteryBoxDetail(Long productId) {
        APIResponse<ExternalMysteryBoxProductDetailVo> externalMysteryBoxProductDetailVoAPIResponse = mysteryBoxExternalApi.queryMysteryBoxDetail(APIRequest.instance(
                ExternalQueryMysteryBoxDetailRequest.builder()
                        .productId(productId)
                        .build()));
        baseHelper.checkResponse(externalMysteryBoxProductDetailVoAPIResponse);
        ExternalMysteryBoxProductDetailVo productDetailVo = externalMysteryBoxProductDetailVoAPIResponse.getData();
        MysteryBoxProductDetailIncludeAssetVo mysteryBoxProductDetailIncludeAssetVo = new MysteryBoxProductDetailIncludeAssetVo();
        BeanUtils.copyProperties(productDetailVo
                , mysteryBoxProductDetailIncludeAssetVo);
        mysteryBoxI18nHelper.doI18n(mysteryBoxProductDetailIncludeAssetVo);
        return new CommonRet<>(mysteryBoxProductDetailIncludeAssetVo);
    }

    @SneakyThrows
    @GetMapping("/mystery-box/query-list-by-serials")
    public CommonRet<List<ExternalMysteryBoxProductSimpleVo>> queryMysteryBoxDetail(String serialsNos) {
        final APIResponse<List<ExternalMysteryBoxProductSimpleVo>> listAPIResponse = mysteryBoxExternalApi.batchQueryMysteryBoxByIds(
                APIRequest.instance(
                        ExternalMysteryBatchQueryByIdRequest.builder()
                                .serialsNos(
                                        Arrays.stream(serialsNos.split(",")).mapToLong(Long::parseLong).boxed().collect(Collectors.toList())
                                ).build()
                )
        );
        baseHelper.checkResponse(listAPIResponse);
        return new CommonRet<>(listAPIResponse.getData());
    }

}
