package com.binance.mgs.business.account.vo;

import com.binance.platform.mgs.business.account.vo.CountryRet;
import lombok.Data;

import java.util.List;

/**
 * @author Men Huatao (alex.men@binance.com)
 * @date 2021/4/13
 */
@Data
public class CountryListWithDefaultRet {
    private CountryRet defaultCountry;
    private List<CountryRet> supportCountryList;
}
