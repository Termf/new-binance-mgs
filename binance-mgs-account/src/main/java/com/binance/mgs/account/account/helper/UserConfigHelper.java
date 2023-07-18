package com.binance.mgs.account.account.helper;

import com.binance.account.api.UserInfoApi;
import com.binance.account.vo.user.request.SetUserConfigRequest;
import com.binance.mgs.account.account.vo.GetUserConfigArg;
import com.binance.mgs.account.account.vo.GetUserConfigRet;
import com.binance.platform.mgs.base.helper.BaseHelper;
import com.binance.platform.mgs.base.vo.CommonRet;
import com.binance.platform.mgs.business.market.LocalRecommendHelper;
import com.binance.platform.mgs.utils.StringUtil;
import com.google.common.collect.Lists;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

/**
 * @Author: mingming.sheng
 * @Date: 2020/4/2 12:13 下午
 */
@Component
@Slf4j
public class UserConfigHelper extends BaseHelper {
    @Resource
    private LocalRecommendHelper localRecommendHelper;
    @Resource
    private UserInfoApi userInfoApi;

    private static final String CONFIG_CURRENCY = "nativeCurrency";

    /**
     * 追加推荐配置
     */
    public void appendRecConfig(GetUserConfigArg arg, CommonRet<List<GetUserConfigRet>> ret) {
        try {
            setRecConfig(arg, ret);
        } catch (Exception e) {
            log.error("UserConfigHelper.appendRecConfig error", e);
        }
    }

    /**
     * 设置推荐配置-汇率
     */
    private void setRecConfig(GetUserConfigArg arg, CommonRet<List<GetUserConfigRet>> ret) throws Exception {
        // 未开启推荐，不推荐
        if (null == arg.getNeedLocalRecommend() || !arg.getNeedLocalRecommend()) {
            return;
        }
        // 指定了配置类型但不是汇率，不推荐
        if (StringUtil.isNotBlank(arg.getConfigType()) && !StringUtil.equals(arg.getConfigType(), CONFIG_CURRENCY)) {
            return;
        }
        // 指定排除了汇率类型，不推荐
        if (StringUtil.isNotBlank(arg.getExclude())) {
            List<String> excludeList = new ArrayList<>(Arrays.asList(arg.getExclude().split(",")));
            if (excludeList.contains(CONFIG_CURRENCY)) {
                return;
            }
        }
        // 返回结果中有汇率，不推荐
        if (!CollectionUtils.isEmpty(ret.getData())) {
            Optional<GetUserConfigRet> currency = ret.getData().stream().filter(x ->
                    StringUtil.equals(x.getConfigType(), CONFIG_CURRENCY)).findFirst();
            if (currency.isPresent()) {
                return;
            }
        }
        // 根据语言推荐汇率
        String recCurrency = localRecommendHelper.getRecCurrency(getLanguage());
        if (StringUtil.isNotBlank(recCurrency)) {
            GetUserConfigRet currencyConfig = new GetUserConfigRet();
            currencyConfig.setConfigType(CONFIG_CURRENCY);
            currencyConfig.setConfigName(recCurrency);
            if (null == ret.getData()) {
                ret.setData(Lists.newArrayList(currencyConfig));
            } else {
                List<GetUserConfigRet> list = new ArrayList<>(ret.getData());
                list.add(currencyConfig);
                ret.setData(list);
            }
        }
    }

    /**
     * 保存用户推荐配置
     */
    public void saveRecConfig(Long userId) {
        try {
            // 优先取cookie中的汇率
            String currency = getCookieValue("userPreferredCurrency");
            if (StringUtil.isBlank(currency) || !currency.endsWith("_USD")) {
                // 没有取推荐汇率
                currency = localRecommendHelper.getRecCurrency(getLanguage());
                if (StringUtil.isBlank(currency)) {
                    return;
                }
            }

            SetUserConfigRequest request = new SetUserConfigRequest();
            request.setUserId(userId);
            request.setConfigType(CONFIG_CURRENCY);
            request.setConfigName(currency);
            userInfoApi.saveUserConfig(getInstance(request));
        } catch (Exception e) {
            log.error("UserConfigHelper.saveRecConfig", e);
        }
    }
}
