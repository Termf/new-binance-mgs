package com.binance.mgs.account.account.vo.subuser;

import com.binance.margin.isolated.api.user.enums.MarginLevelStatus;
import com.binance.margin.isolated.api.user.response.AccountDetailsResponse;
import com.google.common.collect.Lists;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.beans.BeanUtils;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IsolatedMarginAccountDetailsRet {

    @ApiModelProperty("总资产（单位：BTC）")
    private BigDecimal totalAssetOfBtc;

    @ApiModelProperty("总负债（单位：BTC）")
    private BigDecimal totalLiabilityOfBtc;

    @ApiModelProperty("净资产（单位：BTC）")
    private BigDecimal totalNetAssetOfBtc;

    @ApiModelProperty("账户信息列表")
    private List<IsolatedMarginAccountDetail> details;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class IsolatedMarginAccountDetail {

        @ApiModelProperty("基础资产")
        private IsolatedMarginAssetDetail baseAsset;

        @ApiModelProperty("标价货币")
        private IsolatedMarginAssetDetail quoteAsset;

        @ApiModelProperty("杠杆倍率")
        private BigDecimal marginRatio;

        @ApiModelProperty("杠杆率")
        private BigDecimal marginLevel;

        @ApiModelProperty("杠杆率状态")
        private MarginLevelStatus marginLevelStatus;

        @ApiModelProperty("是否可以交易")
        private Boolean tradeEnabled;

        public void transferAsset(AccountDetailsResponse.AssetDetail baseAsset, AccountDetailsResponse.AssetDetail quoteAsset) {
            this.baseAsset = new IsolatedMarginAssetDetail();
            this.quoteAsset = new IsolatedMarginAssetDetail();
            BeanUtils.copyProperties(baseAsset, this.baseAsset);
            BeanUtils.copyProperties(quoteAsset, this.quoteAsset);
        }
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class IsolatedMarginAssetDetail {

        @ApiModelProperty("资产名称")
        private String assetName;

        @ApiModelProperty("可用资产")
        private BigDecimal free;

        @ApiModelProperty("锁定余额")
        private BigDecimal locked;

        @ApiModelProperty("借款")
        private BigDecimal borrowed;

        @ApiModelProperty("利息")
        private BigDecimal interest;

        @ApiModelProperty("净资产")
        private BigDecimal netAsset;

        @ApiModelProperty("净资产（单位：BTC）")
        private BigDecimal netAssetOfBtc;

        @ApiModelProperty("总资产")
        private BigDecimal total;
    }

    public void transferDetails(List<AccountDetailsResponse.AccountDetail> accountDetails) {
        this.details = Lists.newArrayList();
        for (AccountDetailsResponse.AccountDetail src : accountDetails) {
            AccountDetailsResponse.AssetDetail srcBaseAsset = src.getBaseAsset();
            AccountDetailsResponse.AssetDetail srcQuoteAsset = src.getQuoteAsset();
            IsolatedMarginAssetDetail dstBaseAsset = new IsolatedMarginAssetDetail();
            IsolatedMarginAssetDetail dstQuoteAsset = new IsolatedMarginAssetDetail();
            BeanUtils.copyProperties(srcBaseAsset, dstBaseAsset);
            BeanUtils.copyProperties(srcQuoteAsset, dstQuoteAsset);
            IsolatedMarginAccountDetail dst = IsolatedMarginAccountDetail.builder()
                    .baseAsset(dstBaseAsset)
                    .quoteAsset(dstQuoteAsset)
                    .marginRatio(src.getMarginRatio())
                    .marginLevel(src.getMarginLevel())
                    .tradeEnabled(src.getTradeEnabled())
                    .marginLevelStatus(src.getMarginLevelStatus())
                    .build();
            dst.transferAsset(srcBaseAsset, srcQuoteAsset);
            this.details.add(dst);
        }
    }
}
