package com.binance.mgs.account.advice.config;

import com.binance.master.utils.JsonUtils;
import com.binance.master.utils.StringUtils;
import com.ctrip.framework.apollo.ConfigService;
import com.ctrip.framework.apollo.model.ConfigChangeEvent;
import com.ctrip.framework.apollo.spring.annotation.ApolloConfigChangeListener;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author rudy.c
 * @date 2023-03-31 10:46
 */
@Component
@Slf4j
public class OTPSendLimitManager {
    @Value("${otp.send.limit.rule.config:}")
    private String otpSendLimitRuleConfigStr;

    private Map<String, OTPSendLimitRule> otpSendLimitRuleMap = new ConcurrentHashMap<>();

    @PostConstruct
    public void init() {
        refreshRuleConfigMap(otpSendLimitRuleConfigStr);
    }

    @ApolloConfigChangeListener
    public void onApolloChange(ConfigChangeEvent changeEvent) {
        if (changeEvent.isChanged("otp.send.limit.rule.config")) {
            log.info("before refresh otpSendLimitRuleConfigStr = {}", otpSendLimitRuleConfigStr);
            otpSendLimitRuleConfigStr = ConfigService.getAppConfig().getProperty("otp.send.limit.rule.config", otpSendLimitRuleConfigStr);
            log.info("after refresh otpSendLimitRuleConfigStr = {}", otpSendLimitRuleConfigStr);

            refreshRuleConfigMap(otpSendLimitRuleConfigStr);
        }
    }

    private void refreshRuleConfigMap(String otpSendLimitRuleConfigStr) {
        log.info("refreshRuleConfigMap otpSendLimitRuleConfigStr = {}", otpSendLimitRuleConfigStr);
        if (StringUtils.isNotBlank(otpSendLimitRuleConfigStr) && JsonUtils.isJson(otpSendLimitRuleConfigStr)) {
            List<OTPSendLimitRule> ruleList = JsonUtils.toObjList(otpSendLimitRuleConfigStr, OTPSendLimitRule.class);
            Map<String, OTPSendLimitRule> tempOtpSendLimitRuleMap = new ConcurrentHashMap<>(ruleList.size());
            if (CollectionUtils.isNotEmpty(ruleList)) {
                for (OTPSendLimitRule rule : ruleList) {
                    if(rule.validate()) {
                        tempOtpSendLimitRuleMap.put(buildUniqueKey(rule.getOtpType(), rule.getBizScene()), rule);
                    }
                }
            }
            this.otpSendLimitRuleMap = tempOtpSendLimitRuleMap;
        }
    }

    public OTPSendLimitRule getOTPSendLimitRule(String otpType, String bizScene) {
        if(StringUtils.isAnyBlank(otpType, bizScene)) {
            return null;
        }
        return this.otpSendLimitRuleMap.get(buildUniqueKey(otpType, bizScene));
    }

    private String buildUniqueKey(String otpType, String bizScene) {
        return otpType.toUpperCase() + ":" + bizScene.toUpperCase();
    }
}
