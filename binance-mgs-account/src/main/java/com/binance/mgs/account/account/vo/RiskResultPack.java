package com.binance.mgs.account.account.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Description:
 *
 * @author alven
 * @since 2022/12/27
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RiskResultPack {
    /**
     * 命中
     */
    private boolean hit;
    /**
     * 文案
     */
    private String content;
}
