package com.binance.mgs.nft.common.controller;

import com.alibaba.fastjson.JSON;
import com.binance.master.utils.Geoip2Utils;
import com.binance.master.utils.StringUtils;
import com.binance.master.utils.WebUtils;
import com.binance.nft.market.ifae.NftMarketUserApi;
import com.binance.platform.mgs.base.helper.BaseHelper;
import com.binance.platform.mgs.base.vo.CommonRet;
import com.google.common.base.Charsets;
import com.google.common.hash.Hashing;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Set;

/**
 * @Author：Andy L
 * @Date: 2021/6/18 11:06 上午
 */
@Slf4j
@RequestMapping("/v1/public/nft/user")
@RestController
@RequiredArgsConstructor
public class MarketUserController {

    private final NftMarketUserApi marketUserApi;

    private final BaseHelper baseHelper;

    @Value("${nft.ip.block.country.list:[]}")
    private List<String> ipBlockCountryList;

    @Value("${nft.ip.black.country.iso.list:SG}")
    private Set<String> blackCountryIsoSet;

    @Value("${nft.ip.forbidden.switch:false}")
    private Boolean ipForbiddenSwitch;

    @Value("${nft.mgs.nft.dex.switch:false}")
    private Boolean nftDexSwitch;

    @Value("${nft.mgs.nft.dex.gray.number:10}")
    private Integer grayNumber;

    @GetMapping("/forbidden-country/check")
    public CommonRet<Boolean> isUserFromForbiddenCountry(HttpServletRequest request) {
        // switch
        if (!ipForbiddenSwitch) {
            return new CommonRet<>(Boolean.FALSE);
        }
        // get request ip
        String web_utils_ip = WebUtils.getRequestIp();
        // get ip location
        String countryIso = null;
        Geoip2Utils.Geoip2Detail ipResult = Geoip2Utils.getDetail(web_utils_ip);
        if (null != ipResult) {
            countryIso = ipResult.getCountry().getIsoCode();
        }
        log.info("request ip :{} ip location info:{} ip block blackCountryIsoSet:{}", web_utils_ip, JSON.toJSONString(ipResult), blackCountryIsoSet);
        // ip block according Apollo config
        return new CommonRet<>(StringUtils.isNotBlank(countryIso) && blackCountryIsoSet.contains(countryIso));
    }

    @GetMapping("/dex-switch/check")
    public CommonRet<Boolean> checkDexSwitch() {

        if(!nftDexSwitch){
            return new CommonRet<>(false);
        }
        String fVideoId = WebUtils.getHeader("fvideo-id");

        if(StringUtils.isBlank(fVideoId)) {
            return new CommonRet<>(false);
        }
        long hashcode = Math.abs(Hashing.murmur3_128().
                newHasher().putString(fVideoId, Charsets.UTF_8).hash().asLong());
        
        return new CommonRet<>(hashcode % 100 <= grayNumber);
    }
}

