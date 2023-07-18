package com.binance.mgs.account.util;

import com.binance.master.utils.Geoip2Utils;
import com.binance.master.utils.StringUtils;
import com.binance.mgs.business.account.vo.LocationInfo;
import lombok.extern.log4j.Log4j2;

/**
 * Description:
 *
 * @author alven
 * @since 2022/10/10
 */
@Log4j2
public class Ip2LocationSwitchUtils {
    public static String getCountryCity(String ip) {
        String countryCity = "- -";
        try {
            String result = Geoip2Utils.getCountryCity(ip);
            if (StringUtils.isNotBlank(result)) {
                countryCity = result;
            }
            log.info("Ip2LocationSwitchUtils getCountryCity success, ip :{}, result :{}", ip, countryCity);
        } catch (Exception e) {
            log.error("Ip2LocationSwitchUtils getCountryCity error", e);
        }
        return countryCity;
    }

    public static String getCountryShort(String ip){
        String countryShort = "-";
        try{
            String result = Geoip2Utils.getCountryShort(ip);
            if (StringUtils.isNotBlank(result)) {
                countryShort = result;
            }
            log.info("Ip2LocationSwitchUtils getCountryShort success, ip :{}, result :{}", ip, countryShort);
        } catch (Exception e){
            log.error("Ip2LocationSwitchUtils getCountryShort error", e);
        }
        return countryShort;
    }

    public static LocationInfo getDetail(String ip){
        LocationInfo info = LocationInfo.builder().build();
        try {
            String countryShort = Geoip2Utils.getCountryShort(ip);
            if (StringUtils.isNotBlank(countryShort)) {
                info.setCountryShort(countryShort);
            }
            String state = Geoip2Utils.getState(ip);
            if (StringUtils.isNotBlank(state)) {
                info.setRegion(state);
            }
            String city = Geoip2Utils.getCity(ip);
            if (StringUtils.isNotBlank(city)) {
                info.setCity(city);
            }
            log.info("Ip2LocationSwitchUtils getDetail success, ip :{}, result :{}", ip, info);
        } catch (Exception e) {
            log.error("Ip2LocationSwitchUtils getDetail error", e);
        }
        return info;
    }
}
