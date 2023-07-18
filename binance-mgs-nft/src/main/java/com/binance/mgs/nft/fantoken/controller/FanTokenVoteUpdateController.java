package com.binance.mgs.nft.fantoken.controller;

import com.binance.master.models.APIRequest;
import com.binance.master.models.APIResponse;
import com.binance.platform.mgs.base.helper.BaseHelper;
import com.binance.platform.mgs.base.vo.CommonRet;
import com.binance.mgs.nft.fantoken.helper.CacheProperty;
import com.binance.mgs.nft.fantoken.helper.FanTokenCacheHelper;
import com.binance.mgs.nft.mysterybox.helper.FanTokenI18nHelper;
import com.binance.nft.fantoken.ifae.IFanTokenVoteUpdateManageApi;
import com.binance.nft.fantoken.vo.VoteUpdateInfoVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Slf4j
@RequestMapping("/v1/public/nft/fantoken")
@RestController
@RequiredArgsConstructor
public class FanTokenVoteUpdateController {

    private final CacheProperty cacheProperty;
    private final FanTokenCacheHelper fanTokenCacheHelper;

    private final IFanTokenVoteUpdateManageApi voteUpdateManageApi;
    private final BaseHelper baseHelper;
    private final FanTokenI18nHelper fanTokenI18nHelper;

    @GetMapping("/query-update-event-list")
    public CommonRet<List<VoteUpdateInfoVO>> queryUpdateEventByVoteId(@RequestParam String voteId) throws Exception {

        // use cache
        if (cacheProperty.isEnabled()) {
            log.info("query update event use cache: [{}]", voteId);
            List<VoteUpdateInfoVO> voteUpdateInfoVOS = fanTokenCacheHelper.queryUpdateEventByVoteId(voteId);
            fanTokenI18nHelper.doUpdateEventI18n(voteUpdateInfoVOS);
            return new CommonRet<>(voteUpdateInfoVOS);
        } else {
            log.info("query update event use db: [{}]", voteId);
            APIResponse<List<VoteUpdateInfoVO>> response =
                    voteUpdateManageApi.queryUpdateEventByVoteId(APIRequest.instance(voteId));
            baseHelper.checkResponse(response);
            fanTokenI18nHelper.doUpdateEventI18n(response.getData());
            return new CommonRet<>(response.getData());
        }
    }
}
