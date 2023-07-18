package com.binance.mgs.nft.notification.controller;

import com.binance.master.models.APIResponse;
import com.binance.nft.notificationservice.api.INotificationLanguageApi;
import com.binance.platform.mgs.base.helper.BaseHelper;
import com.binance.platform.mgs.base.vo.CommonRet;
import io.swagger.annotations.Api;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Api(tags = "通知使用的语言设置")
@RestController
@RequestMapping("/v1")
@RequiredArgsConstructor
public class NotificationLanguagePreferenceController{
    private final INotificationLanguageApi notificationLanguageApi;
    private final BaseHelper baseHelper;


    @GetMapping("/private/nft/push-center/preference/language/get-selected-language")
    public CommonRet<String> getSelectedLanguage() throws Exception {

        Long userId = baseHelper.getUserId();
        if (null == userId) {
            return new CommonRet<>();
        }

        APIResponse<String> response = notificationLanguageApi.getSelectedLanguage(userId);
        if(response==null){
            return new CommonRet<>();
        }
        return new CommonRet<>(response.getData());
    }


    @GetMapping("/private/nft/push-center/preference/language/update-user-language")
    public CommonRet<Void> updateUserLanguage(@RequestParam String code) throws Exception {

        Long userId = baseHelper.getUserId();
        if (null == userId) {
            return new CommonRet<>();
        }

        notificationLanguageApi.switchUserLanguage(userId,code);
        return new CommonRet<>();
    }
}
