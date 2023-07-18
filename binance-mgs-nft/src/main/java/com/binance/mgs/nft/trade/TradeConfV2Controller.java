package com.binance.mgs.nft.trade;

import com.binance.master.utils.CopyBeanUtils;
import com.binance.master.utils.JsonUtils;
import com.binance.platform.mgs.base.helper.BaseHelper;
import com.binance.platform.mgs.base.helper.CrowdinHelper;
import com.binance.platform.mgs.base.vo.CommonRet;
import com.binance.mgs.nft.trade.proxy.TradeCacheProxy;
import com.binance.nft.tradeservice.constant.Constants;
import com.binance.nft.tradeservice.request.ProductOnsaleConfRequest;
import com.binance.nft.tradeservice.response.ProductOnsaleConfResponse;
import com.binance.nft.tradeservice.utils.Switcher;
import com.binance.nft.tradeservice.vo.CategoryVo;
import io.swagger.annotations.Api;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Api
@Slf4j
@RestController
@RequestMapping("/v2")
public class TradeConfV2Controller {

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
    public CommonRet<ProductOnsaleConfResponse> onsaleConfig(@RequestBody ProductOnsaleConfRequest productOnsaleConfRequest ) throws Exception {
        return new CommonRet<>(this.getOnsaleConfig(productOnsaleConfRequest));
    }

    /**
     * 上架配置
     *
     * @return
     */
    @PostMapping("/friendly/nft/nft-trade/onsale-config")
    public CommonRet<ProductOnsaleConfResponse> onsaleConfig2(@RequestBody ProductOnsaleConfRequest productOnsaleConfRequest ) throws Exception {
        return new CommonRet<>(this.getOnsaleConfig(productOnsaleConfRequest));
    }

    private ProductOnsaleConfResponse getOnsaleConfig(ProductOnsaleConfRequest productOnsaleConfRequest) throws Exception {
        ProductOnsaleConfResponse response = tradeCacheProxy.onsaleConfigV2(productOnsaleConfRequest);
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
        return ret;
    }

}
