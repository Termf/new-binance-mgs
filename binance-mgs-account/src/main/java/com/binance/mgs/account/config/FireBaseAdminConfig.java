package com.binance.mgs.account.config;

import lombok.Data;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
public class FireBaseAdminConfig {
    @Value("${firebase.appcheck.android.project_id:binance-f2287}")
    private String androidProjectId;
    @Value("${firebase.appcheck.android.project_number:826035250503}")
    private String androidProjectNumber;
    @Value("${firebase.appcheck.android.app_id:1:826035250503:android:5ce82cfae8f8e704}")
    private String androidAppId;

    @Value("${firebase.appcheck.ios.project_id:binance-android-ios-dev}")
    private String iosProjectId;
    @Value("${firebase.appcheck.ios.project_number:82847651322}")
    private String iosProjectNumber;
    @Value("${firebase.appcheck.ios.app_id:1:826035250503:android:17ea2998e0d83e13fd1862}")
    private String iosAppId;
}
