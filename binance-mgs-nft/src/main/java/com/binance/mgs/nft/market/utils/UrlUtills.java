package com.binance.mgs.nft.market.utils;


import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public final  class UrlUtills {

    private UrlUtills () {

    }

    private static List<String> imgSuffixs = Arrays.asList("png", "jpg", "jpeg", "gif", "svg");

    private static String replaceAsZipUrl(String url, String suffix) {
       return url.replace("res","zipped").replace("."+suffix,"_zipped.".concat(suffix));
    }

    public static String getZipUrl(String url) {
        if (StringUtils.isBlank(url)) {
            return url;
        }
        String[] split = url.split("\\.");
        if (split.length < 2) {
            return url;
        }
        if (isImgSuffix(split[split.length - 1])) {
            return replaceAsZipUrl(url,split[split.length - 1]);
        }
        return url;

    }

    private static boolean isImgSuffix(String suffix) {
       return CollectionUtils.isNotEmpty(imgSuffixs.stream().filter(item -> item.equalsIgnoreCase(suffix)).collect(Collectors.toList()));
    }
}
