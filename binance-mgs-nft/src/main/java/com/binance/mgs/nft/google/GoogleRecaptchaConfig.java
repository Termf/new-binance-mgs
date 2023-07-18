package com.binance.mgs.nft.google;

import lombok.Data;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import java.util.List;
import java.util.Map;

@Configuration
@Data
public class GoogleRecaptchaConfig {

    @Value("#{'${nft.google.recaptcha.url-list: }'.split(',')}")
    private List<String> googleRecaptureUrlList;

    @Value("${nft.google.recaptcha.score.min:0.9}")
    private Double googleRecaptchaScoreMin;
    @Value("${nft.google.recaptcha.close.status:0}")
    private Integer closeGooleRecaptcha;
    @Value("#{'${nft.google.recaptcha.white-list.user: }'.split(',')}")
    private List<Long> whiteUserIdList;
    @Value("${nft.google.recaptcha.cache.switch:1}")
    private Integer cacheGooleRecaptchaSwitch;

    @Value("${project_id:}")
    private String projectId;
    @Value("${type:}")
    private String type;
    @Value("${private_key_id:}")
    private String private_key_id;
    @Value("${private_key:}")
    private String private_key;
    @Value("${client_email:}")
    private String client_email;
    @Value("${client_id:}")
    private String client_id;
    @Value("${auth_uri:}")
    private String auth_uri;
    @Value("${token_uri:}")
    private String token_uri;
    @Value("${client_x509_cert_url:}")
    private String client_x509_cert_url;
    @Value("${auth_provider_x509_cert_url:}")
    private String auth_provider_x509_cert_url;
    @Value("${nft.google.recaptcha.limit.nextcheck:60}")
    private Long limitNextCheckGooglePeriod;
    @Value("#{${nft.google.recaptcha.url.map.score:}}")
    private Map<String,Double> googleRecapthaMapScore;
    @Value("${nft.google.recaptcha.log.switch:0}")
    private Integer googlelogSwitch;






}
