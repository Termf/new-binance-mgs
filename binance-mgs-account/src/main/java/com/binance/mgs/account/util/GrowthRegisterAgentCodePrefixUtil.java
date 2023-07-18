package com.binance.mgs.account.util;

import com.binance.master.utils.StringUtils;

import java.util.Set;

/**
 * @author sean w
 * @date 2021/11/3
 **/
public class GrowthRegisterAgentCodePrefixUtil {

    public static boolean checkGrowthRegisterAgentCodePrefix(Set<String> prefixs, String agentCode){
        boolean isSatisfiedPrefix = false;
        if (!prefixs.isEmpty()) {
            for (String prefix : prefixs) {
                if (StringUtils.isNotBlank(prefix) && agentCode.startsWith(prefix)) {
                    isSatisfiedPrefix = true;
                    break;
                }
            }
        }
        return isSatisfiedPrefix;
    }
}
