package com.binance.mgs.account;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;

import com.binance.master.constant.Constant;
import com.binance.master.utils.IPUtils;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableDiscoveryClient
@EnableFeignClients(basePackages = { "com.binance.futurestreamer.api","com.binance.deliverystreamer.api","com.binance.account.api", "com.binance.authcenter.api", "com.binance.risk.api", "com.binance.report.api",
        "com.binance.mbxgateway", "com.binance.streamer.api", "com.binance.assetservice.api", "com.binance.fiatpayment.core.api",
        "com.binance.certification.api", "com.binance.marketing.api", "com.binance.future.api", "com.binance.margin.api", "com.binance.c2c.api",
        "com.binance.basedata.api", "com.binance.accountsubuser.api", "com.binance.security.antibot.api", "com.binance.userbigdata.api", "com.binance.broker.api",
        "com.binance.qrcode.api", "com.binance.delivery.periphery.api", "com.binance.accountlog.api", "com.binance.margin.isolated.api", "com.binance.accountsubuserquery.api",
        "com.binance.compliance.api", "com.binance.commission.api", "com.binance.transfer.api", "com.binance.accountdefensecenter.api", "com.binance.accountpersonalcenter.api", "com.binance.assetlog.api", "com.binance.accountvipportal.api",
        "com.binance.capital.api", "com.binance.account2fa.api", "com.binance.fido2.api", "com.binance.accountoauth.api", "com.binance.quota.center.api",
        "com.binance.rule.api", "com.binance.accountapimanage.api", "com.binance.accountmonitorcenter.api","com.binance.oauth.api", "com.binance.accountdevicequery.api",
        "com.binance.accountanalyze.api","com.binance.messaging.api", "com.binance.accountadmin.api","com.binance.rebate.relation.api"
})
@SpringBootApplication(scanBasePackages = {"com.binance.mgs.account", "com.binance.platform.mgs.business.account",
        "com.binance.platform.mgs.business.captcha","com.binance.platform.mgs.business.market"}, exclude = {DataSourceAutoConfiguration.class})
@EnableAsync
@EnableScheduling
public class BinanceMgsApplication {

    public static void main(String[] args) {
        System.setProperty(Constant.LOCAL_IP, IPUtils.getIp());
        SpringApplication.run(BinanceMgsApplication.class, args);
    }

}
