package com.binance.mgs.business.account.vo;

import lombok.Builder;
import lombok.Data;

/**
 * Description:
 *
 * @author alven
 * @since 2022/10/10
 */
@Data
@Builder
public class LocationInfo {
    @Builder.Default
    private String region = "-";
    @Builder.Default
    private String countryShort = "-";
    @Builder.Default
    private String city = "-";
}
