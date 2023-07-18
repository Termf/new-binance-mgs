package com.binance.mgs.nft.fanverse;

import com.binance.master.models.APIRequest;
import com.binance.master.models.APIResponse;
import com.binance.mgs.nft.fantoken.helper.FanTokenCheckHelper;
import com.binance.mgs.nft.fanverse.helper.FanActivityRequestHelper;
import com.binance.mgs.nft.fanverse.helper.FanVerseI18nHelper;
import com.binance.nft.fantoken.activity.ifae.partner.IFanTokenPartnerManagerApi;
import com.binance.nft.fantoken.activity.request.CommonPageRequest;
import com.binance.nft.fantoken.activity.request.FanTokenActivityBaseRequest;
import com.binance.nft.fantoken.activity.request.partner.FanTokenPartnerClaimRequest;
import com.binance.nft.fantoken.activity.request.partner.FanTokenPartnerExternalRequest;
import com.binance.nft.fantoken.activity.response.CommonPageResponse;
import com.binance.nft.fantoken.activity.response.partner.ExternalVerifyResponse;
import com.binance.nft.fantoken.activity.response.partner.FanTokenPartnerBindingUserInfo;
import com.binance.nft.fantoken.activity.response.partner.FanTokenPartnerClaimResponse;
import com.binance.nft.fantoken.activity.response.partner.FanTokenPartnerResponse;
import com.binance.nft.fantoken.activity.response.partner.FanTokenPartnerTaskResponse;
import com.binance.nft.fantoken.activity.vo.bws.utility.BwsCampaignRewardInfo;
import com.binance.platform.mgs.base.helper.BaseHelper;
import com.binance.platform.mgs.base.vo.CommonRet;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Objects;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/v1/friendly/nft/fantoken/partner")
public class FanTokenPartnerController {

    private final BaseHelper baseHelper;
    private final IFanTokenPartnerManagerApi fanTokenPartnerManagerApi;
    private final FanActivityRequestHelper fanActivityRequestHelper;
    private final FanVerseI18nHelper fanVerseI18nHelper;
    private final FanTokenCheckHelper fanTokenCheckHelper;


    @PostMapping("/claim-info")
    public CommonRet<FanTokenPartnerResponse> queryFanTokenPartnerInfo(@RequestBody FanTokenActivityBaseRequest request) {

        fanActivityRequestHelper.initFanTokenBaseRequest(request);
        APIResponse<FanTokenPartnerResponse> response = fanTokenPartnerManagerApi.queryFanTokenPartnerInfo(APIRequest.instance(request));
        baseHelper.checkResponse(response);

        // i18n
        if (Objects.nonNull(response) && Objects.nonNull(response.getData())) {
            fanVerseI18nHelper.doPartnerTeamInfo(response.getData().getPartnerInfo());
        }
        return new CommonRet<>(response.getData());
    }

    @PostMapping("/binding-serial-code")
    public CommonRet<FanTokenPartnerClaimResponse> bindingFanTokenPartnerSerialCode(@RequestBody FanTokenPartnerClaimRequest request) {
        Long userId = baseHelper.getUserId();
        if (userId == null) {
            return new CommonRet<>();
        }
        // SG 校验
        fanTokenCheckHelper.userSGComplianceValidate(userId);

        fanActivityRequestHelper.initFanTokenBaseRequest(request);
        APIResponse<FanTokenPartnerClaimResponse> response = fanTokenPartnerManagerApi.bindingFanTokenPartnerSerialCode(APIRequest.instance(request));
        baseHelper.checkResponse(response);

        return new CommonRet<>(response.getData());
    }

    @PostMapping("/claim-by-serial-code")
    public CommonRet<Void> claimFanTokenPartnerNft(@RequestBody FanTokenPartnerClaimRequest request) {
        Long userId = baseHelper.getUserId();
        if (userId == null) {
            return new CommonRet<>();
        }
        // SG 校验
        fanTokenCheckHelper.userSGComplianceValidate(userId);

        fanActivityRequestHelper.initFanTokenBaseRequest(request);
        APIResponse<Void> response = fanTokenPartnerManagerApi.claimFanTokenPartnerNft(APIRequest.instance(request));
        baseHelper.checkResponse(response);

        return new CommonRet<>();
    }

    @PostMapping("/partner-task-list")
    public CommonRet<FanTokenPartnerTaskResponse> queryFanTokenPartnerTaskInfo(@RequestBody FanTokenActivityBaseRequest request) {

        fanActivityRequestHelper.initFanTokenBaseRequest(request);
        APIResponse<FanTokenPartnerTaskResponse> response = fanTokenPartnerManagerApi.queryFanTokenPartnerTaskInfo(APIRequest.instance(request));
        baseHelper.checkResponse(response);

        // i18n
        if (Objects.nonNull(response) && Objects.nonNull(response.getData()) && CollectionUtils.isNotEmpty(response.getData().getPartnerTaskInfos())) {
            response.getData().getPartnerTaskInfos().forEach(fanVerseI18nHelper::doPartnerTaskInfo);
        }
        return new CommonRet<>(response.getData());
    }

    @PostMapping("/get-binding-user-info")
    public CommonRet<FanTokenPartnerBindingUserInfo> queryPartnerBindingUserInfo(@RequestHeader("activity-signature") String signature,
                                                                                 @RequestBody String body) {
        FanTokenPartnerExternalRequest request = FanTokenPartnerExternalRequest.builder()
                .signature(signature)
                .body(body)
                .build();
        APIResponse<ExternalVerifyResponse<FanTokenPartnerBindingUserInfo>> response = fanTokenPartnerManagerApi.queryPartnerBindingUserInfo(APIRequest.instance(request));
        baseHelper.checkResponse(response);

        ExternalVerifyResponse<FanTokenPartnerBindingUserInfo> responseData = response.getData();
        CommonRet<FanTokenPartnerBindingUserInfo> ret = new CommonRet<>();
        if (response.getData() != null) {
            FanTokenPartnerBindingUserInfo data = response.getData().getData();
            ret.setData(data);
            ret.setCode(responseData.getErrorCode());
            ret.setMessage(responseData.getErrorMessage());
        }
        return ret;
    }

    @PostMapping("/claim-partner-nft-kyc")
    public CommonRet<Void> claimFanTokenPartnerNftNeedKyc(@RequestBody FanTokenPartnerClaimRequest request) {
        Long userId = baseHelper.getUserId();
        if (userId == null) {
            return new CommonRet<>();
        }
        // 强制KYC校验
        fanTokenCheckHelper.userComplianceValidate(userId);
        // SG 校验
        fanTokenCheckHelper.userSGComplianceValidate(userId);

        fanActivityRequestHelper.initFanTokenBaseRequest(userId, request);
        APIResponse<Void> response = fanTokenPartnerManagerApi.claimFanTokenPartnerNft(APIRequest.instance(request));
        baseHelper.checkResponse(response);

        return new CommonRet<>();
    }

    @PostMapping("/user-partner-reward-page")
    public CommonRet<CommonPageResponse<BwsCampaignRewardInfo>> queryUserPartnerRewardPage(@RequestBody CommonPageRequest<FanTokenActivityBaseRequest> request) {
        Long userId = baseHelper.getUserId();
        if (null == userId) {
            return new CommonRet<>();
        }

        FanTokenActivityBaseRequest params = request.getParams();
        if (params == null) {
            params = new FanTokenActivityBaseRequest();
        }
        fanActivityRequestHelper.initFanTokenBaseRequest(userId, params);
        request.setParams(params);
        APIResponse<CommonPageResponse<BwsCampaignRewardInfo>> response = fanTokenPartnerManagerApi.queryUserPartnerRewardPage(APIRequest.instance(request));
        baseHelper.checkResponse(response);
        return new CommonRet<>(response.getData());
    }
}
