package com.binance.mgs.account.account.vo.subuser;

import com.google.common.collect.Maps;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.beans.BeanUtils;

import java.math.BigDecimal;
import java.util.Map;

@ApiModel("isolated-margin盈亏情况返回结果")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProfitSummaryRet {

    @ApiModelProperty("开始时间")
    private Long beginTime;

    @ApiModelProperty("计算时间")
    private Long calcTime;

    @ApiModelProperty("盈亏：<参考系，盈亏数据>")
    private Map<String, ProfitDetail> profits;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ProfitDetail {

        @ApiModelProperty("盈亏额")
        private BigDecimal profit;

        @ApiModelProperty("盈亏率")
        private BigDecimal profitRate;
    }

    public void transferProfits(Map<String, com.binance.margin.isolated.api.profit.response.ProfitDetail> profitDetailMap) {
        this.profits = Maps.newHashMap();
        for (Map.Entry<String, com.binance.margin.isolated.api.profit.response.ProfitDetail> entry : profitDetailMap.entrySet()) {
            com.binance.margin.isolated.api.profit.response.ProfitDetail src = entry.getValue();
            ProfitDetail dst = new ProfitDetail();
            BeanUtils.copyProperties(src, dst);
            this.profits.put(entry.getKey(), dst);
        }
    }
}
