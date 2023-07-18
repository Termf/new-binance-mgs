package com.binance.mgs.nft.inbox;

import com.binance.master.models.APIResponse;
import com.binance.nft.notificationservice.api.INotificationInboxApi;
import com.binance.nft.notificationservice.api.response.InboxUnreadCountResponse;
import com.binance.platform.mgs.base.helper.BaseHelper;
import com.binance.platform.mgs.base.vo.CommonRet;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RequiredArgsConstructor
@RestController
public class InboxController {

    private final BaseHelper baseHelper;
    private final INotificationInboxApi iNotificationInboxApi;

    @GetMapping("/v1/private/nft/inbox/unread-count")
    CommonRet<InboxUnreadCountResponse> getDepositOrderV2() {
        APIResponse<InboxUnreadCountResponse> response = iNotificationInboxApi.getUnreadCount(baseHelper.getUserId());
        baseHelper.checkResponse(response);
        return new CommonRet<>(response.getData());
    }
}
