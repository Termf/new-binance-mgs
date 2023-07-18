package com.binance.mgs.nft;

import com.binance.master.constant.Constant;
import com.binance.master.utils.IPUtils;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.transaction.TransactionAutoConfiguration;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication(exclude = {
        TransactionAutoConfiguration.class,
        DataSourceAutoConfiguration.class
})
@EnableDiscoveryClient
@EnableFeignClients(basePackages = {"com.binance.nft", "com.binance.nft.mintservice.api.iface", "com.binance.nft.reconcilication.api",
        "com.binance.nft.assetservice.api","com.binance.nftcore"})
@EnableAsync
@EnableScheduling
public class BinanceNFTMgsApplication {

    public static void main(String[] args) {
        System.setProperty(Constant.LOCAL_IP, IPUtils.getIp());
        SpringApplication.run(BinanceNFTMgsApplication.class, args);
    }
}
