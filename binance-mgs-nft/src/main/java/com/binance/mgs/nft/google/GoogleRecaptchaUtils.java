package com.binance.mgs.nft.google;

import com.amazonaws.util.StringInputStream;
import com.binance.master.utils.HttpClientUtils;
import com.binance.master.utils.JsonUtils;
import com.binance.mgs.nft.common.controller.model.GoogleRecaptchaResponse;
import com.google.auth.oauth2.AccessToken;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.auth.oauth2.ServiceAccountCredentials;
import com.google.common.collect.Lists;
import lombok.extern.slf4j.Slf4j;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@Slf4j
public class GoogleRecaptchaUtils {


    private static String path = "https://recaptchaenterprise.googleapis.com/v1/projects/%s/assessments";

    public static String getGooleAuthorizationToken(String recaptchaPluginAuth) throws IOException {
        GoogleCredentials scoped = ServiceAccountCredentials.fromStream(new StringInputStream
                (recaptchaPluginAuth)).createScoped(
                Lists.newArrayList("https://www.googleapis.com/auth/cloud-platform"));
        AccessToken accessToken = scoped.refreshAccessToken();
        return accessToken.getTokenValue();
    }



    /**
     * 接口注释
     * @param params
     * @param googleAuthToken
     * @param projectId
     * @param googleRecaptchaScoreMin
     * @param googlelogSwitch
     * @return
     */
    public static boolean checkGoogleRecaptcha(Map<String, Object> params, String googleAuthToken, String projectId, Double googleRecaptchaScoreMin, Integer googlelogSwitch)  {
        try {
            HashMap<String, String> headerMap = new HashMap<>();
            headerMap.put("Authorization", "Bearer " + googleAuthToken);
            String content = HttpClientUtils.postJson(String.format(path, projectId), params, headerMap);
            if(googlelogSwitch == 1) {
                log.error("checkGoogleRecaptcha content params = " + content);
            }
            GoogleRecaptchaResponse response = JsonUtils.toObj(content, GoogleRecaptchaResponse.class);
            if (response == null) {
                return Boolean.FALSE;
            }

            return  null != response.getRiskAnalysis()  && null != response.getRiskAnalysis().getScore() && response.getRiskAnalysis().getScore() >= googleRecaptchaScoreMin;
        }catch (Exception e) {
            log.error("checkGoogleRecaptcha error params = " + JsonUtils.toJsonNotNullKey(params));
            return Boolean.FALSE;
        }
    }
}
