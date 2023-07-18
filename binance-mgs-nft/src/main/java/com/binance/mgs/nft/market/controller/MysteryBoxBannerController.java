package com.binance.mgs.nft.market.controller;

import com.binance.master.models.APIRequest;
import com.binance.master.models.APIResponse;
import com.binance.mgs.nft.mysterybox.controller.MysteryBoxController;
import com.binance.mgs.nft.mysterybox.vo.MysteryBoxProductDto;
import com.binance.nft.assetservice.api.data.dto.CommonRet;
import com.binance.nft.market.ifae.NFTMysteryBoxBannerAdminApi;
import com.binance.nft.market.request.mysteryboxbanner.MysteryBoxBannerUpdateRequest;
import com.binance.nft.market.vo.CommonPageRequest;
import com.binance.nft.market.vo.CommonPageResponse;
import com.binance.nftcore.utils.lambda.check.BaseHelper;
import com.binance.platform.mgs.base.vo.CommonPageRet;
import com.binance.platform.mgs.config.CaffeineCacheConfig;
import lombok.RequiredArgsConstructor;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.function.Function;
import java.util.stream.Collectors;

@RestController
@RequiredArgsConstructor
@RequestMapping("/v1/public/nft/mystery-box-all")
public class MysteryBoxBannerController {

    private final NFTMysteryBoxBannerAdminApi nftMysteryBoxBannerAdminApi;

    private final MysteryBoxController mysteryBoxController;

    @GetMapping("/list")
    @Cacheable(value = CaffeineCacheConfig.DEFAULT_CACHE_SMALL, key = "'getMysteryBoxBanner-'+#page+'-'+#size")
    public CommonRet<CommonPageResponse<MysteryBoxProductDto>> getMysteryBoxBanner(@RequestParam("page") Integer page, @RequestParam("size") Integer size) throws ExecutionException {
        //通过运营配置得到banner
        CommonPageResponse<MysteryBoxBannerUpdateRequest> configs = getMysteryBoxBannerByConfig(page, size);
        Map<Integer, MysteryBoxBannerUpdateRequest> configMap = CollectionUtils.emptyIfNull(configs.getData()).stream()
                .collect(Collectors.toMap(MysteryBoxBannerUpdateRequest::getSortNumber, Function.identity(), (v1, v2) -> v1));

        int total = page * size;
        List<MysteryBoxProductDto> mysteryBoxBannerMgsVos = new ArrayList<>();
        //通过一级市场自动补位
        CommonPageRet<MysteryBoxProductDto> mysteryBoxProductDto = mysteryBoxController.listMysteryBox(page, size, 1);
        int defaultSize = Optional.ofNullable(mysteryBoxProductDto.getData()).map(List::size).orElse(0);
        int cur = 0;
        for (int i = 1; i <= total; i++) {
            if (configMap.containsKey(i)) {
                MysteryBoxProductDto configProduct = new MysteryBoxProductDto();
                configProduct.setConfigType(3);
                configProduct.setName(configMap.get(i).getName());
                configProduct.setImage(configMap.get(i).getCoverUrl());
                configProduct.setSubTitle(configMap.get(i).getSubTitle());
                configProduct.setTitle(configMap.get(i).getTitle());
                configProduct.setPageLink(configMap.get(i).getPageLink());
                mysteryBoxBannerMgsVos.add(configProduct);
            } else if (cur < defaultSize) {
                mysteryBoxBannerMgsVos.add(mysteryBoxProductDto.getData().get(cur));
                cur += 1;
            }
        }
        mysteryBoxBannerMgsVos = mysteryBoxBannerMgsVos.subList((page - 1) * size, Math.min(page * size, mysteryBoxBannerMgsVos.size()));
        CommonPageResponse<MysteryBoxProductDto> response = CommonPageResponse.<MysteryBoxProductDto>builder()
                .data(mysteryBoxBannerMgsVos)
                .total(mysteryBoxBannerMgsVos.size())
                .size(size)
                .page(page)
                .build();
        return CommonRet.<CommonPageResponse<MysteryBoxProductDto>>builder()
                .data(response)
                .success(Boolean.TRUE)
                .build();
    }

    private CommonPageResponse<MysteryBoxBannerUpdateRequest> getMysteryBoxBannerByConfig(Integer page, Integer size) {
        CommonPageRequest<Boolean> request = CommonPageRequest.<Boolean>builder()
                .params(Boolean.FALSE)
                .page(page)
                .size(size)
                .build();
        APIResponse<CommonPageResponse<MysteryBoxBannerUpdateRequest>> response = nftMysteryBoxBannerAdminApi.getItemListDetail(APIRequest.instance(request));
        BaseHelper.checkResponse(response);
        return response.getData();
    }
}
