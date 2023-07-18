package com.binance.mgs.account.config;

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.auth.EC2ContainerCredentialsProviderWrapper;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.sqs.AmazonSQS;
import com.amazonaws.services.sqs.AmazonSQSClientBuilder;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Log4j2
@Configuration
public class AmazonSqsConfig {

  @Value("${aws.sqs.region.name:ap-northeast-1}")
  private String awsSqsRegionName;
  @Value("${env:dev}")
  private String currtentEnv;
  @Value("${com.binance.secretsManager.accessKey:}")
  private String accessKey;
  @Value("${com.binance.secretsManager.secretKey:}")
  private String secretKey;

  @Bean
  public AmazonSQS amazonSQS() {
    AmazonSQS amazonSQS = null;
    Regions region = Regions.fromName(awsSqsRegionName);
    try{
      if ("dev".equalsIgnoreCase(currtentEnv) && StringUtils.isNoneBlank(accessKey, secretKey)) {
        // 本地启动直接使用账号密码
        AWSCredentials credentials = new BasicAWSCredentials(accessKey, secretKey);
        amazonSQS = AmazonSQSClientBuilder.standard().withCredentials(new AWSStaticCredentialsProvider(credentials)).withRegion(region).build();
        log.info("AmazonSqsConfig:  amazonSQS success by ak,sk");
      } else {
        // 基于role的方式启动，除了dev环境以外全部强制用role的方式来启动
        amazonSQS = AmazonSQSClientBuilder.standard().withCredentials(new EC2ContainerCredentialsProviderWrapper())
                .withRegion(region).build();
        log.info("AmazonSqsConfigs:  amazonSQS success by role");
      }
    }catch (Exception e){
      log.error("amazonSQS init error ",e);
    }

    return amazonSQS;
  }
}
