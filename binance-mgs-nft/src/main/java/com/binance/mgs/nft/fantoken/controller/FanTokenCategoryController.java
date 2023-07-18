package com.binance.mgs.nft.fantoken.controller;

import com.alibaba.fastjson.JSON;
import com.binance.master.models.APIRequest;
import com.binance.master.models.APIResponse;
import com.binance.platform.mgs.base.helper.BaseHelper;
import com.binance.platform.mgs.base.vo.CommonRet;
import com.binance.mgs.nft.fantoken.helper.FanTokenCacheHelper;
import com.binance.mgs.nft.mysterybox.helper.FanTokenI18nHelper;
import com.binance.nft.fantoken.ifae.IFanTokenCategoryManagerApi;
import com.binance.nft.fantoken.request.QueryNftMarketRequest;
import com.binance.nft.fantoken.request.category.CategoryDisplayRequest;
import com.binance.nft.fantoken.response.QueryNftMarketResponse;
import com.binance.nft.fantoken.vo.CategoryDisplayVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@RequestMapping("/v1/friendly/nft/fantoken")
@RestController
@RequiredArgsConstructor
public class FanTokenCategoryController {

    private final IFanTokenCategoryManagerApi categoryManagerApi;
    private final BaseHelper baseHelper;
    private final FanTokenI18nHelper fanTokenI18nHelper;
    private final FanTokenCacheHelper cacheHelper;

    private static final String EDITION_NOT_ON_SALE = "nftType=null&orderBy=amount_sort&orderType=1&serialNo=%s&tradeType=0";
    private static final String EDITION_ON_SALE = "keyword=%s&nftType=null&orderBy=amount_sort&orderType=1&serialNo=%s&tradeType=0";

    /**
     * <h2>Team Category 展示</h2>
     * 1. 不是登录状态, cache(guava) 有效时长 1 分钟
     * 2. 是登录状态, cache(redis) 有效时长 1 分钟
     * */
    @PostMapping("/collection/display")
    public CommonRet<List<CategoryDisplayVO>> displayCategoryInfoByTeamId(@RequestBody CategoryDisplayRequest request)
            throws Exception {

        Long userId = baseHelper.getUserId();
        request.setUserId(userId);  // 可以是 null
        log.info("category display: [{}]", JSON.toJSONString(request));

        // 不是登录状态, guava cache 1min
        if (null == userId) {
            // 由于涉及到多张数据表的读取操作, 可能配置没有那么及时, 如果读取的数据不完整, 清除掉 Guava Cache
            List<CategoryDisplayVO> result = cacheHelper.queryTeamCategoryInfo(request.getTeamId());
            fanTokenI18nHelper.doCategoryI18n(result);
            tryToInvalidateCategoryDisplayCache(request.getTeamId(), result);
            return new CommonRet<>(result);
        } else {
            // 是登录状态
            // 1. 获取的数据中包含有用户的 NFT 资产数据
            // 2. 需要根据用户的资产构造市场信息
            APIResponse<List<CategoryDisplayVO>> response =
                    categoryManagerApi.displayCategoryInfoByTeamId(APIRequest.instance(request));   // 1 分钟 redis cache
            baseHelper.checkResponse(response);
            fanTokenI18nHelper.doCategoryI18n(response.getData());

            // 填充市场信息
            // 以 batchId + keywords 构造 10 分钟的缓存
            // 用户点击 NFT 查看时, 由前端去查询并展示市场信息
//            fillingCategoryMarketInfoForLoginUser(response.getData());

            return new CommonRet<>(response.getData());
        }
    }

    /**
     * <h2>对于登录用户, 需要填充市场信息</h2>
     * */
    public void fillingCategoryMarketInfoForLoginUser(List<CategoryDisplayVO> categoryDisplayVOS) {

        if (CollectionUtils.isEmpty(categoryDisplayVOS)) {
            return;
        }

        Map<String, Set<String>> batchIds2Keywords = new HashMap<>();
        categoryDisplayVOS.forEach(cd -> {
            if (CollectionUtils.isNotEmpty(cd.getCategorySets())) {
                cd.getCategorySets().forEach(cs -> {
                    Set<String> keywords = getEditionNamesFromCategorySet(cs);
                    if (!batchIds2Keywords.containsKey(cs.getBatchId())) {
                        batchIds2Keywords.put(cs.getBatchId(), new HashSet<>());
                    }
                    batchIds2Keywords.get(cs.getBatchId()).addAll(keywords);
                });
            }
        });

        List<QueryNftMarketResponse> nftMarketResponses = new ArrayList<>(batchIds2Keywords.size());

        if (MapUtils.isNotEmpty(batchIds2Keywords)) {
            List<QueryNftMarketRequest> requests = new ArrayList<>(batchIds2Keywords.size());
            batchIds2Keywords.forEach((k, v) -> requests.add(new QueryNftMarketRequest(
                    k,
                    v.stream().sorted().collect(Collectors.toList())    // 排序之后保持每次顺序都是一样的
            )));

            // TODO 目前不会配置多个 batchId, 这里暂时使用 for 循环的方式完成
            requests.forEach(r -> {
                try {
                    nftMarketResponses.add(cacheHelper.queryNftMarketLoadingCache(r));
                } catch (Exception ex) {
                    log.error("get nft market response error: [{}]", ex.getMessage(), ex);
                }
            });
        }

        // 填充市场参数
        log.info("query nft market response: [{}]", JSON.toJSONString(nftMarketResponses));
        fillingCategoryMarketInfoForLoginUser(categoryDisplayVOS, nftMarketResponses);
    }

    /**
     * <h2>填充市场参数信息</h2>
     * */
    private void fillingCategoryMarketInfoForLoginUser(List<CategoryDisplayVO> categoryDisplayVOS,
                                                       List<QueryNftMarketResponse> nftMarketResponses) {

        Map<String, List<QueryNftMarketResponse.KeywordSize>> batchId2KeywordSize = nftMarketResponses.stream()
                .collect(Collectors.toMap(QueryNftMarketResponse::getSerialsNo, QueryNftMarketResponse::getKeywordSizes));
        categoryDisplayVOS.forEach(cd -> {
            if (CollectionUtils.isNotEmpty(cd.getCategorySets())) {
                cd.getCategorySets().forEach(cs -> fillingEditionSaleInfo(cs.getBatchId(), cs.getItems(),
                        batchId2KeywordSize.getOrDefault(cs.getBatchId(), Collections.emptyList())));
            }
        });
    }

    /**
     * <h2>填充 Items 的市场售卖信息</h2>
     * */
    private void fillingEditionSaleInfo(String batchId, List<CategoryDisplayVO.CategorySetsDTO.ItemsDTO> items,
                                        List<QueryNftMarketResponse.KeywordSize> keywordSizes) {

        // 从市场中获取到了售卖信息, 但是仍然可能存在不售卖的 Edition
        Map<String, Integer> keyword2Count = keywordSizes.stream().collect(Collectors.toMap(
                QueryNftMarketResponse.KeywordSize::getKeyword, QueryNftMarketResponse.KeywordSize::getSize
        ));
        items.forEach(i -> {
            if (CollectionUtils.isNotEmpty(i.getEditionInfos())) {
                String notOnSaleMarketParam = String.format(EDITION_NOT_ON_SALE, batchId);
                i.getEditionInfos().forEach(e -> {
                    if (keyword2Count.getOrDefault(e.getEditionName(), 0) > 0) {
                        e.setMarketParam(String.format(EDITION_ON_SALE, e.getEditionName(), batchId));
                    } else {
                        e.setMarketParam(notOnSaleMarketParam);
                    }
                });
            }
        });
    }

    /**
     * <h2>从 CategorySetsDTO 中获取 Edition Name</h2>
     * */
    private Set<String> getEditionNamesFromCategorySet(CategoryDisplayVO.CategorySetsDTO categorySetsDTO) {

        Set<String> keywords = new HashSet<>();

        if (CollectionUtils.isNotEmpty(categorySetsDTO.getItems())) {
            categorySetsDTO.getItems().forEach(i -> {
                if (CollectionUtils.isNotEmpty(i.getEditionInfos())) {
                    keywords.addAll(i.getEditionInfos().stream()
                            .map(CategoryDisplayVO.CategorySetsDTO.ItemsDTO.EditionInfosDTO::getEditionName)
                            .collect(Collectors.toSet()));
                }
            });
        }

        return keywords;
    }

    /**
     * <h2>尝试清除 CategoryDisplay Guava Cache</h2>
     * */
    private void tryToInvalidateCategoryDisplayCache(String teamId, List<CategoryDisplayVO> categoryDisplayVOS) {

        if (CollectionUtils.isEmpty(categoryDisplayVOS)) {
            cacheHelper.invalidateCategoryDisplayCache(teamId);
            return;
        }

        boolean invalidateFlag = false;
        for (CategoryDisplayVO cd : categoryDisplayVOS) {
            if (CollectionUtils.isEmpty(cd.getCategorySets())) {
                invalidateFlag = true;
                break;
            } else {
                for (CategoryDisplayVO.CategorySetsDTO cs : cd.getCategorySets()) {
                    if (CollectionUtils.isEmpty(cs.getItems())) {
                        invalidateFlag = true;
                        break;
                    } else {
                        for (CategoryDisplayVO.CategorySetsDTO.ItemsDTO item : cs.getItems()) {
                            if (CollectionUtils.isEmpty(item.getEditionInfos())) {
                                invalidateFlag = true;
                                break;
                            }
                        }
                    }
                }
            }
        }
        if (invalidateFlag) {
            cacheHelper.invalidateCategoryDisplayCache(teamId);
        }
    }
}
