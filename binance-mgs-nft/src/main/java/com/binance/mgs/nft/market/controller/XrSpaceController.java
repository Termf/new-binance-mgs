package com.binance.mgs.nft.market.controller;

import com.binance.master.constant.Constant;
import com.binance.master.models.APIResponse;
import com.binance.master.utils.WebUtils;
import com.binance.platform.mgs.base.helper.BaseHelper;
import com.binance.platform.mgs.base.vo.CommonRet;
import com.binance.platform.mgs.config.CaffeineCacheConfig;
import com.binance.mgs.nft.market.proxy.XrSpaceCacheProxy;
import com.binance.nft.market.ifae.XrSpaceApi;
import com.binance.nft.market.vo.xrspace.XrExhibitionListVo;
import com.binance.nft.market.vo.xrspace.XrRoomListVo;
import com.binance.nft.market.vo.xrspace.XrSpaceListVo;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RequestMapping("/v1/public/nft/")
@RestController
@RequiredArgsConstructor
public class XrSpaceController {

    private final XrSpaceCacheProxy xrSpaceCacheProxy;

    @GetMapping("/xrspace")
    @Cacheable(value = CaffeineCacheConfig.DEFAULT_CACHE_MIDDLE)
    public CommonRet<XrSpaceListVo> xrspaceList() throws Exception {
        XrSpaceListVo ret = xrSpaceCacheProxy.xrspaceList();
        return new CommonRet<>(ret);
    }

    @GetMapping("/xrspace/{spaceNo}")
    @Cacheable(value = CaffeineCacheConfig.DEFAULT_CACHE_MIDDLE)
    public CommonRet<XrRoomListVo> roomList(@PathVariable("spaceNo") Long spaceNo) {
        XrRoomListVo ret = xrSpaceCacheProxy.roomList(spaceNo);
        return new CommonRet<>(ret);
    }


    @GetMapping("/xrspace/{spaceNo}/{roomNo}")
    @Cacheable(value = CaffeineCacheConfig.DEFAULT_CACHE_MIDDLE)
    public CommonRet<XrExhibitionListVo> exhibitionList(@PathVariable("spaceNo") Long spaceNo, @PathVariable("roomNo") Long roomNo) {
        XrExhibitionListVo ret = xrSpaceCacheProxy.exhibitionList(spaceNo, roomNo);
        return new CommonRet<>(ret);
    }


}
