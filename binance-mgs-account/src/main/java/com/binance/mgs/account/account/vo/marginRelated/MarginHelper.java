package com.binance.mgs.account.account.vo.marginRelated;

import com.binance.master.models.APIResponse;
import com.binance.platform.mgs.base.vo.CommonPageRet;
import com.binance.platform.mgs.base.vo.CommonRet;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * @author sean w
 * @date 2021/10/20
 **/
public class MarginHelper {

    private static final String NO_OPEN_MARGIN_ACCOUNT = "051002";
    private static final String NO_OPEN_ISOLATED_MARGIN_ACCOUNT = "128003";
    public static final String MARGIN_BUSINESS_EXCEPTION_CODE_INPUT_PARAMS_VALIDATED = "051026";

    private static final Set<String> NO_OPEN_ACCOUNT_CODES = Sets.newHashSet(
            NO_OPEN_MARGIN_ACCOUNT, NO_OPEN_ISOLATED_MARGIN_ACCOUNT);

    private static final String ZERO_STRING = "0.00000000";

    public static final List<String> MARKETS = Lists.newArrayList("BTC", "USDT");

    public static final CommonRet<String> ZERO_RESPONSE = new CommonRet<>(ZERO_STRING);

    public static final CommonRet<BigDecimalWrapper> ZERO_WRAPPER_RESPONSE =
            new CommonRet<>(BigDecimalWrapper.of(BigDecimal.ZERO));

    @SuppressWarnings("rawtypes")
    public static final CommonPageRet EMPTY_RESPONSE = new CommonPageRet<>(Collections.emptyList(), 0L);

    @SuppressWarnings("unchecked")
    public static <T> CommonPageRet<T> emptyResponse() {
        return (CommonPageRet<T>) EMPTY_RESPONSE;
    }

    public static boolean isNotOpenAccount(APIResponse<?> resp) {
        return Objects.nonNull(resp)
                && resp.getStatus() != APIResponse.Status.OK
                && NO_OPEN_ACCOUNT_CODES.contains(resp.getCode());
    }
}
