package com.binance.mgs.account.account.vo.marginRelated;

import com.binance.master.models.APIRequest;
import com.binance.master.models.APIResponse;
import com.binance.platform.mgs.base.vo.CommonPageRet;
import com.binance.platform.mgs.base.vo.CommonRet;
import com.binance.platform.mgs.business.AbstractBaseAction;

import java.math.BigDecimal;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Function;

/**
 * @author sean w
 * @date 2021/10/20
 **/
public abstract class MarginBaseAction extends AbstractBaseAction {

    protected String toString(BigDecimal value) {
        if (value != null) {
            return value.setScale(8, BigDecimal.ROUND_HALF_EVEN).toPlainString();
        }
        return null;
    }

    protected String toString(Long value) {
        if (value != null) {
            return value.toString();
        }
        return null;
    }

    protected String toString(Integer value) {
        if (value != null) {
            return value.toString();
        }
        return null;
    }


    protected <T> CommonRet<T> ok(APIResponse<T> response) {
        checkResponse(response);
        return new CommonRet<>(response.getData());
    }


    protected <T> CommonPageRet<T> ok(List<T> list, long total) {
        return new CommonPageRet<>(list, total);
    }

    protected <R, P> CommonRet<R> execute(Function<APIRequest<P>, APIResponse<R>> apiFunction, BiConsumer<P, Long> requiredUserId, P body) {
        requiredUserId.accept(body, checkAndGetUserId());
        APIResponse<R> response = apiFunction.apply(APIRequest.instance(body));
        checkResponse(response);
        return new CommonRet<>(response.getData());
    }
}
