package com.binance.mgs.nft.notification.controller;

import com.binance.master.models.APIResponse;
import com.binance.mgs.nft.nftasset.controller.helper.UserHelper;
import com.binance.nft.notificationservice.api.INotificationChannelApi;
import com.binance.nft.notificationservice.api.response.NotificationChannelResponse;
import com.binance.platform.mgs.base.helper.BaseHelper;
import com.binance.platform.mgs.base.helper.CrowdinHelper;
import com.binance.platform.mgs.base.vo.CommonRet;
import io.swagger.annotations.Api;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;

@Api(tags = "通知项的设置")
@RestController
@RequestMapping("/v1")
@RequiredArgsConstructor
public class NotificationChannelPreferenceController  {
    private final INotificationChannelApi notificationChannelApi;
    private final CrowdinHelper crowdinHelper;
    private final BaseHelper baseHelper;
    private final UserHelper userHelper;

    @GetMapping("/private/nft/push-center/preference/config/select")
    public CommonRet<List<NotificationChannelResponse>> allChannels() throws Exception {

        Long userId = baseHelper.getUserId();
        if (null == userId) {
            return new CommonRet<>();
        }

        String lang = baseHelper.getLanguage();
        APIResponse<List<NotificationChannelResponse>> response = notificationChannelApi.channelList(userId);
        if(response==null){
            return new CommonRet<>();
        }
        List<NotificationChannelResponse> responseList = response.getData();
        List<NotificationChannelResponse> result = new ArrayList<>(responseList.size());
        responseList.forEach(notificationChannelResponse->{
            //对于mint开关的显示进行白名单判断
            if (notificationChannelResponse.getId()==1l && !userHelper.checkUserWhiteList(userId).isMintFlag()){
                return;
            }
            notificationChannelResponse.setLang(lang);
            notificationChannelResponse.setIntroduction(crowdinHelper.getMessageByKey(notificationChannelResponse.getLabel()+"-introduction",lang));
            notificationChannelResponse.setLabel(crowdinHelper.getMessageByKey(notificationChannelResponse.getLabel()+"-label",lang));
            result.add(notificationChannelResponse);
        });
        return new CommonRet<>(result);
    }


    @GetMapping("/private/nft/push-center/preference/config/update")
    public CommonRet<Void> updateUserChannel(@RequestParam Long id,@RequestParam Boolean status) throws Exception {

        Long userId = baseHelper.getUserId();
        if (null == userId) {
            return new CommonRet<>();
        }

        notificationChannelApi.switchUserChannel(userId,status,id);
        return new CommonRet<>();
    }
}
