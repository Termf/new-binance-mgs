package com.binance.mgs.nft.market.controller;

import com.binance.master.utils.JsonUtils;
import com.binance.platform.mgs.base.helper.BaseHelper;
import com.binance.platform.mgs.base.vo.CommonRet;
import lombok.RequiredArgsConstructor;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RequestMapping("/v1/private/nft")
@RestController
@RequiredArgsConstructor
public class MarketWhiteListController {

    private final BaseHelper baseHelper;

    @Value("${nft.pre-minting.page.white-list:[]}")
    private String preMintingPageWhiteList;

    @GetMapping("/pre-mint-access")
    public CommonRet<Boolean> isInMintingWhiteList() throws Exception {
        Boolean result = Boolean.FALSE;
        String userId = baseHelper.getUserIdStr();
        List<String> strings = JsonUtils.toObjList(preMintingPageWhiteList, String.class);
        if (CollectionUtils.isNotEmpty(strings)) {
            result = strings.stream().anyMatch(userId::equalsIgnoreCase);
        }
        return new CommonRet<>(result);
    }
}
