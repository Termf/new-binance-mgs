package com.binance.mgs.account.oauth.controller;

import com.binance.accountdefensecenter.core.annotation.CallAppCheck;
import com.binance.master.models.APIRequest;
import com.binance.master.models.APIResponse;
import com.binance.master.utils.CopyBeanUtils;
import com.binance.mgs.account.AccountBaseAction;
import com.binance.mgs.account.account.vo.ClientDetailUserIdArg;
import com.binance.mgs.account.account.vo.oauth.OauthGetBindUserArg;
import com.binance.mgs.account.account.vo.oauth.OauthGetBindUserRet;
import com.binance.mgs.account.account.vo.oauth.OauthGetOpenIdArg;
import com.binance.mgs.account.account.vo.OauthUnbindUserArg;
import com.binance.mgs.account.account.vo.oauth.OauthGetOpenIdRet;
import com.binance.mgs.account.oauth.helper.OauthHelper;
import com.binance.oauth.api.OauthBindUserApi;
import com.binance.oauth.api.OauthClientApi;
import com.binance.oauth.vo.BindUserRequest;
import com.binance.oauth.vo.ClientDetailUserIdRequest;
import com.binance.oauth.vo.ClientDetailUserIdResponse;
import com.binance.oauth.vo.OauthBindUserResponse;
import com.binance.oauth.vo.OauthUnbindUserRequest;
import com.binance.oauth.vo.OauthUnbindUserResponse;
import com.binance.platform.mgs.base.vo.CommonRet;
import com.binance.platform.mgs.utils.PKGenarator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.List;
import java.util.Objects;

@RestController
@RequestMapping(value = "/v1")
@Slf4j
public class OAuthController extends AccountBaseAction {
    @Resource
    private OauthHelper oauthHelper;

    @Autowired
    private OauthBindUserApi oauthBindUserApi;

    @Autowired
    private OauthClientApi oauthClientApi;

    @GetMapping(value = "/public/oauth/code")
    @CallAppCheck(value = "OAuthController.submitAuthorizationCode")
    @Deprecated
    public CommonRet<String> submitAuthorizationCode(HttpServletRequest request, HttpServletResponse response) throws Exception {
        if (isBinanceCom()) {
            // 主站不需要这个逻辑
            log.debug("skip .com");
            return ok();
        }
        // save stats firstly
        String callback = request.getParameter("callback");
        log.debug("submitAuthorizationCode [state : {}]", callback);
        String state = PKGenarator.getId();
        OauthHelper.saveState(state, callback);
        response.sendRedirect(oauthHelper.getAuthorizeCodeUri(state, baseHelper.getLanguage(), request.getParameter("region")));
        return new CommonRet<>();
    }

    @PostMapping(value = "/private/oauth/queryClientDetailsByUserId")
    public CommonRet<List<ClientDetailUserIdResponse>> queryClientDetailsByUserId(@Validated @RequestBody ClientDetailUserIdArg clientDetailUserIdArg) throws Exception {
        ClientDetailUserIdRequest clientDetailUserIdRequest = CopyBeanUtils.fastCopy(clientDetailUserIdArg, ClientDetailUserIdRequest.class);
        clientDetailUserIdRequest.setUserId(checkAndGetUserId());
        APIResponse<List<ClientDetailUserIdResponse>> listAPIResponse = oauthClientApi.queryClientDetailsByUserId(APIRequest.instance(clientDetailUserIdRequest));
        if (!baseHelper.isOk(listAPIResponse)) {
            checkResponse(listAPIResponse);
        }
        List<ClientDetailUserIdResponse> data = listAPIResponse.getData();
        return new CommonRet<>(data);
    }

    @PostMapping(value = "/private/oauth/unbindOauth")
    public CommonRet<OauthUnbindUserResponse> unbindOauth(@Validated @RequestBody OauthUnbindUserArg oauthUnbindUserArg) throws Exception {
        OauthUnbindUserRequest oauthUnbindUserRequest = CopyBeanUtils.fastCopy(oauthUnbindUserArg, OauthUnbindUserRequest.class);
        oauthUnbindUserRequest.setUserId(checkAndGetUserId());
        APIResponse<OauthUnbindUserResponse> oauthUnbindUserResponseAPIResponse = oauthBindUserApi.unbindOauth(APIRequest.instance(oauthUnbindUserRequest));
        if (!baseHelper.isOk(oauthUnbindUserResponseAPIResponse)) {
            checkResponse(oauthUnbindUserResponseAPIResponse);
        }

        OauthUnbindUserResponse data = oauthUnbindUserResponseAPIResponse.getData();
        return new CommonRet<>(data);
    }

    @PostMapping(value = "/private/oauth/getOpenId")
    public CommonRet<OauthGetOpenIdRet> getOpenId(@Validated @RequestBody OauthGetOpenIdArg oauthGetOpenIdArg) {
        BindUserRequest bindUserRequest = new BindUserRequest();
        bindUserRequest.setUserId(checkAndGetUserId());
        bindUserRequest.setClientId(oauthGetOpenIdArg.getClientId());
        APIResponse<OauthBindUserResponse> response = oauthBindUserApi.getOrCreateByUserId(APIRequest.instance(bindUserRequest));
        if (!baseHelper.isOk(response)) {
            checkResponse(response);
        }
        OauthBindUserResponse data = response.getData();
        OauthGetOpenIdRet ret = new OauthGetOpenIdRet();
        ret.setOpenId(data.getOpenId());
        return new CommonRet<>(ret);
    }

    @PostMapping(value = "/private/oauth/getBindUser")
    public CommonRet<OauthGetBindUserRet> getBindUser(@Validated @RequestBody OauthGetBindUserArg arg) {
        BindUserRequest bindUserRequest = new BindUserRequest();
        bindUserRequest.setUserId(checkAndGetUserId());
        bindUserRequest.setClientId(arg.getClientId());
        APIResponse<OauthBindUserResponse> response = oauthBindUserApi.getByUserId(APIRequest.instance(bindUserRequest));
        if (!baseHelper.isOk(response)) {
            checkResponse(response);
        }
        OauthBindUserResponse data = response.getData();
        OauthGetBindUserRet ret = new OauthGetBindUserRet();
        if (Objects.nonNull(data)) {
            ret.setScope(data.getScope());
        }
        return new CommonRet<>(ret);
    }
}
