package com.binance.mgs.account.account.helper;

import com.binance.account.api.CountryApi;
import com.binance.account.vo.country.CountryStatusVo;
import com.binance.account.vo.country.CountryVo;
import com.binance.account.vo.country.GetCountryByCodeRequest;
import com.binance.account.vo.country.GetMobileRegisterSupportCountryListeRequest;
import com.binance.account.vo.country.GetMobileRegisterSupportCountryListeResp;
import com.binance.account.vo.country.MobileRegisterSupportCountryListResp;
import com.binance.master.models.APIRequest;
import com.binance.master.models.APIResponse;
import com.binance.master.utils.JsonUtils;
import com.binance.master.utils.WebUtils;
import com.binance.platform.mgs.base.helper.BaseHelper;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.Maps;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.TimeUnit;

@Component
@Slf4j
public class AccountCountryHelper extends BaseHelper {

    @Autowired
    private CountryApi countryApi;

    // terminal --> countrylist
    @Getter
    private LoadingCache<String, GetMobileRegisterSupportCountryListeResp> mobileRegisterCountryListCache =
            CacheBuilder.newBuilder().maximumSize(20).expireAfterWrite(600, TimeUnit.SECONDS).build(new CacheLoader<String, GetMobileRegisterSupportCountryListeResp>() {
                @Override
                public GetMobileRegisterSupportCountryListeResp load(String terminalCode) {
                    GetMobileRegisterSupportCountryListeRequest request = new GetMobileRegisterSupportCountryListeRequest();
                    request.setIp(WebUtils.getRequestIp());
                    APIResponse<GetMobileRegisterSupportCountryListeResp> apiResponse = countryApi.getMobileRegisterSupportCountryList(getInstance(request));
                    checkResponse(apiResponse);
                    return apiResponse.getData();
                }
            });

    // terminal --> countrylist
    @Getter
    private LoadingCache<String, MobileRegisterSupportCountryListResp> mobileRegisterCountryListCacheV2 =
            CacheBuilder.newBuilder().maximumSize(20).expireAfterWrite(600, TimeUnit.SECONDS).build(new CacheLoader<String, MobileRegisterSupportCountryListResp>() {
                @Override
                public MobileRegisterSupportCountryListResp load(String terminalCode) {
                    GetMobileRegisterSupportCountryListeRequest request = new GetMobileRegisterSupportCountryListeRequest();
                    request.setIp(WebUtils.getRequestIp());
                    APIResponse<MobileRegisterSupportCountryListResp> apiResponse = countryApi.getMobileRegisterSupportCountryListV2(getInstance(request));
                    checkResponse(apiResponse);
                    return apiResponse.getData();
                }
            });

    private LoadingCache<String, Map<String, String>> countryRegionMapCache =
            CacheBuilder.newBuilder().maximumSize(20).expireAfterWrite(600, TimeUnit.SECONDS).build(new CacheLoader<String, Map<String, String>>() {
                @Override
                public Map<String, String> load(String key) {
                    GetMobileRegisterSupportCountryListeRequest request = new GetMobileRegisterSupportCountryListeRequest();
                    request.setIp(WebUtils.getRequestIp());
                    APIResponse<MobileRegisterSupportCountryListResp> apiResponse = countryApi.getMobileRegisterSupportCountryListV2(getInstance(request));
                    checkResponse(apiResponse);

                    MobileRegisterSupportCountryListResp resp = apiResponse.getData();
                    Map<String, String> countryRegionMap = Maps.newHashMap();
                    for (CountryStatusVo countryStatusVo : resp.getCountryList()) {
                        countryRegionMap.put(countryStatusVo.getCode(), countryStatusVo.getRegion());
                    }
                    return countryRegionMap;
                }
            });

    // mobileCode
    private LoadingCache<String, CountryVo> countryVoLoadingCache =
            CacheBuilder.newBuilder().maximumSize(300).expireAfterWrite(2, TimeUnit.HOURS).build(new CacheLoader<String, CountryVo>() {
                @Override
                public CountryVo load(String mobileCode) throws Exception {
                    try {
                        GetCountryByCodeRequest request = new GetCountryByCodeRequest();
                        request.setCode(mobileCode);
                        APIResponse<CountryVo> response = countryApi.getCountryByCode(APIRequest.instance(request));
                        boolean ok = isOk(response);
                        if (!ok || response.getData() == null || response.getData().getMobileCode() == null) {
                            log.warn("getCountryByCode failed {}", JsonUtils.toJsonHasNullKey(response));
                            return null;
                        }
                        return response.getData();
                    } catch (Exception e) {
                        log.warn("getCountryByCode failed ", e);
                        return null;
                    }
                }
            });


    public CountryVo getCountry(String mobileCode) {
        try {
            return countryVoLoadingCache.get(mobileCode);
        } catch (Exception e) {
            log.warn("getCountry from cache failed", e);
            return null;
        }
    }

    public String getRegionByCountryCode(String countryCode) {
        if (StringUtils.isBlank(countryCode)) {
            return "";
        }
        try {
            Map<String, String> countryRegionMap = countryRegionMapCache.get("");
            return countryRegionMap.getOrDefault(countryCode, "");
        } catch (Exception e) {
            log.warn("getRegionByCountryCode error", e);
            return "";
        }
    }
}
