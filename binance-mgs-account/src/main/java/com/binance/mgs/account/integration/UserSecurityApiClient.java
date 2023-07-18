package com.binance.mgs.account.integration;

import com.binance.account.api.UserSecurityApi;
import com.binance.account.vo.security.UserSecurityVo;
import com.binance.account.vo.security.request.UserIdRequest;
import com.binance.master.error.BusinessException;
import com.binance.master.error.GeneralCode;
import com.binance.master.models.APIRequest;
import com.binance.master.models.APIResponse;
import com.binance.platform.mgs.base.helper.BaseHelper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

/**
 * Description:
 *
 * @author alven
 * @since 2023/5/3
 */
@Slf4j
@Component
public class UserSecurityApiClient {
    @Resource
    private UserSecurityApi userSecurityApi;
    
    @Resource
    private BaseHelper baseHelper;
    
    public UserSecurityVo getUserSecurityByUserId(Long userId) throws Exception {
        UserIdRequest userIdRequest = new UserIdRequest();
        userIdRequest.setUserId(userId);
        APIResponse<UserSecurityVo> userSecurityVoResponse = userSecurityApi.getUserSecurityByUserId(APIRequest.instance(userIdRequest));
        baseHelper.checkResponse(userSecurityVoResponse);
        if (userSecurityVoResponse.getData() == null) {
            throw new BusinessException(GeneralCode.ILLEGAL_PARAM);
        }
        return userSecurityVoResponse.getData();
    }
}
