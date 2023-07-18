package com.binance.mgs.nft.trade;

import com.alibaba.fastjson.JSONObject;
import com.binance.master.utils.CopyBeanUtils;
import com.binance.master.utils.JsonUtils;
import com.binance.mgs.nft.trade.request.BatchOnsaleFeeRequest;
import com.binance.nft.tradeservice.request.BatchOnsaleFeeConfigRequest;
import com.binance.nft.tradeservice.request.BatchProductOnsaleRequest;
import com.binance.nft.tradeservice.request.ProductOnsaleConfRequest;
import com.binance.nft.tradeservice.response.BatchNftInfoListConfig;
import com.binance.nft.tradeservice.vo.CollectionOlConfigVo;
import com.binance.platform.mgs.base.helper.BaseHelper;
import com.binance.platform.mgs.base.helper.CrowdinHelper;
import com.binance.platform.mgs.base.vo.CommonRet;
import com.binance.mgs.nft.trade.proxy.TradeCacheProxy;
import com.binance.nft.tradeservice.constant.Constants;
import com.binance.nft.tradeservice.enums.ListTypeEnum;
import com.binance.nft.tradeservice.enums.SourceTypeEnum;
import com.binance.nft.tradeservice.request.ProductFeeRequest;
import com.binance.nft.tradeservice.response.ProductOnsaleConfResponse;
import com.binance.nft.tradeservice.utils.Switcher;
import com.binance.nft.tradeservice.vo.CategoryVo;
import com.binance.nft.tradeservice.vo.ProductFeeVo;
import io.swagger.annotations.Api;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.MessageFormat;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Api
@Slf4j
@RestController
@RequestMapping("/v1")
public class TradeConfController {

    @Resource
    private BaseHelper baseHelper;
    @Resource
    private CrowdinHelper crowdinHelper;
    @Resource
    private TradeCacheProxy tradeCacheProxy;

    /**
     * 上架配置
     *
     * @return
     */
    @PostMapping("/public/nft/nft-trade/onsale-config")
    public CommonRet<ProductOnsaleConfResponse> onsaleConfig() throws Exception {
        ProductOnsaleConfResponse response = tradeCacheProxy.onsaleConfig();

        // front i18n
        Set<Integer> categorySet = response.getCategoryList().stream()
                .map(CategoryVo::getCode)
                .collect(Collectors.toSet());
        String json = crowdinHelper.getMessageByKey(Constants.TRADE_MARKET_CATEGORYS_KEY, baseHelper.getLanguage());
        List<CategoryVo> categoryList = Switcher.<List<CategoryVo>>Case()
                .when(!Objects.equals(json, Constants.TRADE_MARKET_CATEGORYS_KEY))
                .then(() -> JsonUtils.toObjList(json, CategoryVo.class)
                        .stream().filter(c -> categorySet.contains(c.getCode()))
                        .collect(Collectors.toList())
                )
                .end(response.getCategoryList());
        ProductOnsaleConfResponse ret = CopyBeanUtils.fastCopy(response, ProductOnsaleConfResponse.class);
        ret.setCategoryList(categoryList);

        return new CommonRet<>(ret);
    }


    /**
     * 上架费率, nft list fee
     *
     * @return
     */
    @PostMapping("/private/nft/nft-trade/onsale-fee")
    public CommonRet<ProductFeeVo> onsaleFee(@Valid @RequestBody ProductFeeRequest request) throws Exception {
        log.info("/private/nft/nft-trade/onsale-fee request:{}", JSONObject.toJSONString(request));
        ProductFeeVo settleFee = tradeCacheProxy.onsaleFee(request);

        // front i18n
        ProductFeeVo result = Switcher.<ProductFeeVo>Case().when(SourceTypeEnum.WEB.typeEquals(request.getSource()) &&
                ListTypeEnum.PUBLIC_MARKET.typeEquals(request.getListType()))
                .then(() -> {
                    ProductFeeVo feeVo = CopyBeanUtils.fastCopy(settleFee, ProductFeeVo.class);
                    feeVo.setRemark(getRemarkMessage(feeVo).toString());
                    return feeVo;
                }).end(settleFee);

        return new CommonRet<>(result);
    }

    @PostMapping("/public/nft/nft-trade/onsale-config-list")
    public CommonRet<Map<Long, BatchNftInfoListConfig>> onSaleConfigList(@Valid @RequestBody BatchOnsaleFeeRequest request) {
        return new CommonRet<>(this.getOnSaleConfigList(request));
    }

    @PostMapping("/friendly/nft/nft-trade/onsale-config-list")
    public CommonRet<Map<Long, BatchNftInfoListConfig>> onSaleConfigList2(@Valid @RequestBody BatchOnsaleFeeRequest request) {
        return new CommonRet<>(this.getOnSaleConfigList(request));
    }

    @PostMapping("/public/nft/nft-trade/openlisting/onsale-info")
    public CommonRet<Map<Integer, CollectionOlConfigVo>> olOnSaleInfo(@RequestBody ProductOnsaleConfRequest request) throws Exception {
        Map<Integer, CollectionOlConfigVo> response = tradeCacheProxy.olOnSaleInfo(request);
        return new CommonRet<>(response);
    }

    @PostMapping("/public/nft/nft-trade/openlisting/onsale-info-list")
    public CommonRet<Map<Long, List<CollectionOlConfigVo>>> olOnSaleInfoList(@Valid @RequestBody BatchOnsaleFeeConfigRequest request) throws Exception {
        Map<Long, List<CollectionOlConfigVo>> response = tradeCacheProxy.olOnSaleInfoList(request);
        return new CommonRet<>(response);
    }

    @NotNull
    private StringBuffer getRemarkMessage(ProductFeeVo feeVo) {

        DecimalFormat df = new DecimalFormat("0%");
        BigDecimal royaltiesFee = feeVo.getRoyaltiesFee() != null ? feeVo.getRoyaltiesFee() : new BigDecimal(0);
        BigDecimal platformFee = feeVo.getPlatformFee() != null ? feeVo.getPlatformFee() : new BigDecimal(0);
        StringBuffer i18Msg = new StringBuffer();
        if (platformFee.compareTo(BigDecimal.ZERO) > 0) {
            String platformMsg = crowdinHelper.getMessageByKey("nft-market-fee-0002-listing", baseHelper.getLanguage());
            platformMsg = MessageFormat.format(platformMsg, df.format(platformFee));
            i18Msg.append(platformMsg);
        } else {
            String platformMsg = crowdinHelper.getMessageByKey("nft-market-fee-0004-listing", baseHelper.getLanguage());
            i18Msg.append(platformMsg);
        }
        if (royaltiesFee.compareTo(BigDecimal.ZERO) > 0) {
            String royalties18Msg = crowdinHelper.getMessageByKey("nft-market-fee-0003-listing", baseHelper.getLanguage());
            royalties18Msg = MessageFormat.format(royalties18Msg, df.format(royaltiesFee));
            i18Msg.append(royalties18Msg);
        } else {
            String royalties18Msg = crowdinHelper.getMessageByKey("nft-market-fee-0005-listing", baseHelper.getLanguage());
            i18Msg.append(royalties18Msg);
        }
        return i18Msg;
    }

    private Map<Long, BatchNftInfoListConfig> getOnSaleConfigList(BatchOnsaleFeeRequest request){
        Map<Long, BatchNftInfoListConfig> settleFeeMap = tradeCacheProxy.batchOnsaleConfig(request);
        settleFeeMap.forEach((key, value) -> {
            ProductFeeVo feeVo = value.getFeeVo();
            feeVo.setRemark(getRemarkMessage(feeVo).toString());
            value.setFeeVo(feeVo);
        });
        return settleFeeMap;
    }
}
