package com.binance.mgs.account.account.helper;

import com.binance.assetservice.api.IAssetApi;
import com.binance.assetservice.vo.response.asset.AssetResponse;
import com.binance.assetservice.vo.response.asset.GetAssetPicResponse;
import com.binance.master.models.APIRequest;
import com.binance.master.models.APIResponse;
import com.binance.platform.mgs.base.helper.BaseHelper;
import com.binance.platform.mgs.component.domain.CommonDomainHelper;
import com.binance.platform.mgs.utils.ListTransformUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;

/**
 * copy from {@link com.binance.mgs.business.asset.helper.AssetHelper}
 *
 * @author kvicii
 * @date 2021/06/08
 */
@Component
@Slf4j
public class AccountAssetHelper extends BaseHelper {
    @Resource
    private IAssetApi assetApi;

    @Resource
    private CommonDomainHelper commonDomainHelper;

    //防止如果有logo无法显示
    @Value("${chinaLogoOpen:true}")
    private Boolean chinaLogoOpen;
    @Value("${oldDomain:https://bin.bnbstatic.com}")
    private String oldDomain;
    @Value("${oldDomain2:https://ex.bnbstatic.com}")
    private String oldDomain2;

    private List<AssetResponse> chinaCDNAsset = new ArrayList<>();
    private List<AssetResponse> globalCDNAsset = new ArrayList<>();
    private List<GetAssetPicResponse> chinaCDNAssetPic = new ArrayList<>();
    private List<GetAssetPicResponse> globalCDNAssetPic = new ArrayList<>();

    /**
     * 1分钟刷一次
     */
    @Scheduled(fixedRate = 60 * 1000, initialDelay = 10 * 1000)
    protected void timedRefresh() {
        try {
            this.loadAssetData();
        } catch (Exception e) {
            log.error("AccountAssetHelper.timedRefresh", e);
        }
    }

    private void loadAssetData() throws Exception {
        // 国外URL
        globalCDNAsset = getAllAsset();
        //国内URL
        chinaCDNAsset = ListTransformUtil.transform(globalCDNAsset, AssetResponse.class);

        // 国外Pic
        globalCDNAssetPic = new ArrayList<>();
        createAssetPic(globalCDNAsset, globalCDNAssetPic);

        String cnDomain = commonDomainHelper.getCDNUrlByRegion("CN");
        chinaCDNAsset.forEach(data -> {
            if (StringUtils.isNotBlank(data.getLogoUrl())) {
                if (data.getLogoUrl().contains(oldDomain)) {
                    data.setLogoUrl(data.getLogoUrl().replace(oldDomain, cnDomain));
                    data.setFullLogoUrl(data.getLogoUrl());
                } else if (data.getLogoUrl().contains(oldDomain2)) {
                    data.setLogoUrl(data.getLogoUrl().replace(oldDomain2, cnDomain));
                    data.setFullLogoUrl(data.getLogoUrl());
                }
            }
        });
        // 国内pic
        chinaCDNAssetPic = new ArrayList<>();
        createAssetPic(chinaCDNAsset, chinaCDNAssetPic);
    }

    private void createAssetPic(List<AssetResponse> assetList, List<GetAssetPicResponse> assetPicResponses) {
        assetList.forEach(asset -> {
            GetAssetPicResponse resp = new GetAssetPicResponse();
            // URL都是http开头的，简化asset-service if逻辑
            resp.setPic(asset.getLogoUrl());
            resp.setAsset(asset.getAssetCode());
            assetPicResponses.add(resp);
        });
    }

    /**
     * 获取资产
     *
     * @return
     * @throws Exception
     */
    private List<AssetResponse> getAllAsset() throws Exception {
        APIResponse<List<AssetResponse>> assetResponse = assetApi.getAllAsset(APIRequest.instanceBodyNull());
        checkResponse(assetResponse);
        return assetResponse.getData();
    }

    public List<AssetResponse> getAllAssetResponseByTopDomain(String topDomain) throws Exception {
        checkInitValue();
        if (chinaLogoOpen && StringUtils.isNotEmpty(topDomain) && commonDomainHelper.isChinaCDN(topDomain)) {
            return chinaCDNAsset;
        } else {
            return globalCDNAsset;
        }
    }

    private void checkInitValue() throws Exception {
        if (CollectionUtils.isEmpty(chinaCDNAsset) || CollectionUtils.isEmpty(globalCDNAsset)) {
            log.info("chinaCDNAsset is empty");
            globalCDNAsset = getAllAsset();
            chinaCDNAsset = globalCDNAsset;
        }
    }
}
