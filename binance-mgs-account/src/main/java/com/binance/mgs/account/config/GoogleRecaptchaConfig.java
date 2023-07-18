package com.binance.mgs.account.config;

import lombok.Data;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
public class GoogleRecaptchaConfig {
    @Value("${private_key_id:}")
    private String private_key_id;
    @Value("${private_key:}")
    private String private_key;
    @Value("${project_id:}")
    private String projectId;
    @Value("${type:}")
    private String type;
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
}
