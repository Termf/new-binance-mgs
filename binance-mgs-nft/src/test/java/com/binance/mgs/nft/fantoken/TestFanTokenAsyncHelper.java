package com.binance.mgs.nft.fantoken;

import com.alibaba.fastjson.JSON;
import com.binance.mgs.nft.fantoken.controller.FanTokenCategoryController;
import com.binance.mgs.nft.fantoken.helper.FanTokenAsyncHelper;
import com.binance.mgs.nft.fantoken.helper.FanTokenCacheHelper;
import com.binance.mgs.nft.fantoken.helper.FanTokenCheckHelper;
import com.binance.nft.fantoken.request.QueryNftMarketRequest;
import com.binance.nft.fantoken.response.QueryNftMarketResponse;
import com.binance.nft.fantoken.vo.CategoryDisplayVO;
import com.google.common.base.Stopwatch;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Slf4j
@SpringBootTest
@RunWith(SpringRunner.class)
public class TestFanTokenAsyncHelper {

    @Autowired
    private FanTokenAsyncHelper fanTokenAsyncHelper;
    @Autowired
    private FanTokenCacheHelper fanTokenCacheHelper;
    @Autowired
    private FanTokenCategoryController fanTokenCategoryController;
    @Autowired
    private FanTokenCheckHelper fanTokenCheckHelper;

//    @Test
    public void testUserHasKyc() {

        Long userId = 354879353L;
        log.info("user has kyc: {}", fanTokenCheckHelper.hasKyc(userId));
    }

//    @Test
    public void testQueryNftMarket() {

        Stopwatch stopwatch = Stopwatch.createStarted();
        QueryNftMarketResponse response = fanTokenAsyncHelper.queryNftMarket(buildRequest());
        log.info("query nft market elapsed: [{}]", stopwatch.elapsed(TimeUnit.MILLISECONDS));
        log.info("query nft market response: [{}]", JSON.toJSONString(response));
    }

//    @Test
    @SneakyThrows
    public void testQueryNftMarketByGuavaCache() {

        for (int i = 0; i != 10; ++i) {
            Stopwatch stopwatch = Stopwatch.createStarted();
            log.info("query cache: [{}]",
                    JSON.toJSONString(fanTokenCacheHelper.queryNftMarketLoadingCache(buildRequest())));
            log.info("query nft market elapsed: [{}]", stopwatch.elapsed(TimeUnit.MILLISECONDS));
        }
    }

//    @Test
    public void testQueryNftMarketController() {

        List<CategoryDisplayVO> categoryDisplayVOS = new ArrayList<>();
        CategoryDisplayVO x = new CategoryDisplayVO();

        CategoryDisplayVO.CategorySetsDTO d1 = buildCategorySetsDTO("100001", Arrays.asList("a", "b", "c"));
        CategoryDisplayVO.CategorySetsDTO d2 = buildCategorySetsDTO("100002", Arrays.asList("x", "y", "z"));
        CategoryDisplayVO.CategorySetsDTO d3 = buildCategorySetsDTO("100003", Arrays.asList("a", "b", "c"));
        CategoryDisplayVO.CategorySetsDTO d4 = buildCategorySetsDTO("100001", Arrays.asList("c", "e", "d"));

        x.setCategorySets(Arrays.asList(d1, d2, d3, d4));
        categoryDisplayVOS.add(x);

        log.info("test request: [{}]", JSON.toJSONString(categoryDisplayVOS));

        fanTokenCategoryController.fillingCategoryMarketInfoForLoginUser(categoryDisplayVOS);
        log.info("test response: [{}]", JSON.toJSONString(categoryDisplayVOS));
    }

    private CategoryDisplayVO.CategorySetsDTO buildCategorySetsDTO(String batchId, List<String> keywords) {

        CategoryDisplayVO.CategorySetsDTO a = new CategoryDisplayVO.CategorySetsDTO();
        a.setBatchId(batchId);
        List<CategoryDisplayVO.CategorySetsDTO.ItemsDTO> aItems = new ArrayList<>();
        List<CategoryDisplayVO.CategorySetsDTO.ItemsDTO.EditionInfosDTO> editionInfos = new ArrayList<>();
        keywords.forEach(x -> {
            CategoryDisplayVO.CategorySetsDTO.ItemsDTO.EditionInfosDTO tmp =
                    new CategoryDisplayVO.CategorySetsDTO.ItemsDTO.EditionInfosDTO();
            tmp.setEditionName(x);
            editionInfos.add(tmp);
        });
        aItems.add(new CategoryDisplayVO.CategorySetsDTO.ItemsDTO("", editionInfos));
        a.setItems(aItems);

        return a;
    }

    private QueryNftMarketRequest buildRequest() {

        QueryNftMarketRequest request = new QueryNftMarketRequest();
        request.setSerialsNo("134260683398322176");
        request.setKeywords(Arrays.asList("Bronze R Kid Capaldi", "Bronze Big Fat Sexy", "Bronze Mr Fashion"));
        return request;
    }
}
