package com.binance.mgs.nft.fantoken.controller;

import com.binance.master.models.APIRequest;
import com.binance.master.models.APIResponse;
import com.binance.master.utils.StringUtils;
import com.binance.mgs.nft.fantoken.helper.FanTokenCheckHelper;
import com.binance.mgs.nft.mysterybox.helper.FanTokenI18nHelper;
import com.binance.nft.fantoken.ifae.singleticket.IUserSingleTicketCertificateManageApi;
import com.binance.nft.fantoken.request.CommonPageRequest;
import com.binance.nft.fantoken.request.singleticket.UserSingleTicketCertificateRequest;
import com.binance.nft.fantoken.request.ticketnft.TicketNftEventCardDetailPageRequest;
import com.binance.nft.fantoken.response.singleticket.SingleTicketCardDetailPageResponse;
import com.binance.nft.fantoken.response.singleticket.UserSingleTicketCertificateResponse;
import com.binance.nft.fantoken.response.singleticket.UserSubmitSingleTicketCertificateResponse;
import com.binance.nft.fantoken.vo.singleticket.SingleTicketCardRewardVO;
import com.binance.nft.fantoken.vo.ticketnft.TicketNftCardDetailVO;
import com.binance.nft.fantoken.vo.ticketnft.TicketNftCardInformationVO;
import com.binance.platform.mgs.base.helper.BaseHelper;
import com.binance.platform.mgs.base.vo.CommonRet;
import com.google.common.base.Preconditions;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@SuppressWarnings("all")
@Slf4j
@RequestMapping("/v1/friendly/nft/fantoken/ticketnft")
@RestController
@RequiredArgsConstructor
public class UserSingleTicketCertificateController {

    private final BaseHelper baseHelper;
    private final FanTokenI18nHelper fanTokenI18nHelper;
    private final FanTokenCheckHelper fanTokenCheckHelper;
    private final IUserSingleTicketCertificateManageApi userSingleTicketCertificateManageApi;

    /**
     * <h2>给 UserTicketCertificateRequest 填充登录用户信息</h2>
     * */
    private void fillingUserInfoToUserTicketCertificateRequest(Long userId, UserSingleTicketCertificateRequest request) {

        request.setIsGray(fanTokenCheckHelper.isGray());
        request.setUserId(userId);
        request.setClientType(baseHelper.getClientType());
        request.setLocale(baseHelper.getLanguage());
        request.setHasKyc(null != userId && fanTokenCheckHelper.hasKyc(userId));
    }

    @PostMapping("/submit-singleticket-number")
    public CommonRet<UserSubmitSingleTicketCertificateResponse> submitSingleTicketCertificate(@RequestBody UserSingleTicketCertificateRequest request) {

        Long userId = baseHelper.getUserId();
        if (null == userId || null == request) {
            return new CommonRet<>();
        }

        fillingUserInfoToUserTicketCertificateRequest(userId, request);
        request.setComplianceAssetDto(fanTokenCheckHelper.fanTokenComplianceAsset(baseHelper.getUserId()));
        APIResponse<UserSubmitSingleTicketCertificateResponse> response = userSingleTicketCertificateManageApi
                .submitSingleTicketCertificate(APIRequest.instance(request));
        baseHelper.checkResponse(response);

        return new CommonRet<>(response.getData());
    }

    @PostMapping("/singleticket-query-nft-distribution")
    public CommonRet<UserSingleTicketCertificateResponse> querySingleTicketDistribution(@RequestBody UserSingleTicketCertificateRequest request) {

        Long userId = baseHelper.getUserId();
        if (null == userId || null == request) {
            return new CommonRet<>();
        }

        fillingUserInfoToUserTicketCertificateRequest(userId, request);
        APIResponse<UserSingleTicketCertificateResponse> response = userSingleTicketCertificateManageApi
                .querySingleTicketDistribution(APIRequest.instance(request));
        baseHelper.checkResponse(response);

        return new CommonRet<>(response.getData());
    }

    @PostMapping("/singleticket-event-config")
    public CommonRet<TicketNftCardDetailVO> singleTicketEventConfig(@RequestBody UserSingleTicketCertificateRequest request) {

        // 不需要登录
        fillingUserInfoToUserTicketCertificateRequest(baseHelper.getUserId(), request);
        APIResponse<TicketNftCardDetailVO> response = userSingleTicketCertificateManageApi
                .singleTicketEventConfig(APIRequest.instance(request));
        baseHelper.checkResponse(response);

        // i18n
        if (null != response.getData()) {
            fanTokenI18nHelper.doTicketNftCardDetailVO(response.getData());
        }

        return new CommonRet<>(response.getData());
    }

    @PostMapping("/europaleague-event-config")
    public CommonRet<TicketNftCardDetailVO> europaLeagueEventConfig(@RequestBody UserSingleTicketCertificateRequest request) {

        // 不需要登录
        fillingUserInfoToUserTicketCertificateRequest(baseHelper.getUserId(), request);
        APIResponse<TicketNftCardDetailVO> response = userSingleTicketCertificateManageApi
                .europaLeagueEventConfig(APIRequest.instance(request));
        baseHelper.checkResponse(response);

        // i18n
        if (null != response.getData()) {
            fanTokenI18nHelper.doTicketNftCardDetailVO(response.getData());
        }

        return new CommonRet<>(response.getData());
    }

    @PostMapping("/coppaitalia-event-config")
    public CommonRet<TicketNftCardDetailVO> coppaItaliaEventConfig(@RequestBody UserSingleTicketCertificateRequest request) {

        // 不需要登录
        fillingUserInfoToUserTicketCertificateRequest(baseHelper.getUserId(), request);
        APIResponse<TicketNftCardDetailVO> response = userSingleTicketCertificateManageApi.coppaItaliaEventConfig(
                APIRequest.instance(request));
        baseHelper.checkResponse(response);

        // i18n
        if (null != response.getData()) {
            fanTokenI18nHelper.doTicketNftCardDetailVO(response.getData());
        }

        return new CommonRet<>(response.getData());
    }

    @PostMapping("/singleticket-card-detail")
    public CommonRet<TicketNftCardDetailVO> singleTicketCardDetail(@RequestBody UserSingleTicketCertificateRequest request) {

        Long userId = baseHelper.getUserId();
        if (null == userId || null == request) {
            return new CommonRet<>();
        }

        fillingUserInfoToUserTicketCertificateRequest(userId, request);
        APIResponse<TicketNftCardDetailVO> response = userSingleTicketCertificateManageApi
                .singleTicketCardDetail(APIRequest.instance(request));
        baseHelper.checkResponse(response);

        // i18n
        if (null != response.getData()) {
            fanTokenI18nHelper.doTicketNftCardDetailVO(response.getData());
        }

        return new CommonRet<>(response.getData());
    }

    @PostMapping("/singleticket-card-details")
    public CommonRet<SingleTicketCardDetailPageResponse> singleTicketCardDetails(@RequestBody CommonPageRequest<TicketNftEventCardDetailPageRequest> request) {

        Long userId = baseHelper.getUserId();
        if (null == userId
                || null == request || null == request.getParams() || null == request.getParams().getQuery()) {
            return new CommonRet<>();
        }

        Preconditions.checkArgument(StringUtils.isNotBlank(request.getParams().getQuery().getTeamId()),
                "teamId is invalid");

        // 填充 userId
        request.getParams().getQuery().setUserId(userId);

        APIResponse<SingleTicketCardDetailPageResponse> response = userSingleTicketCertificateManageApi
                .singleTicketCardDetails(APIRequest.instance(request));
        baseHelper.checkResponse(response);

        // i18n
        if (null != response.getData()) {
            fanTokenI18nHelper.doSingleTicketCardDetailPageResponse(response.getData());
        }

        return new CommonRet<>(response.getData());
    }

    @PostMapping("/singleticket-card-information")
    public CommonRet<TicketNftCardInformationVO> singleTicketCardInformation(@RequestBody UserSingleTicketCertificateRequest request) {

        Long userId = baseHelper.getUserId();
        if (null == userId || null == request) {
            return new CommonRet<>();
        }

        fillingUserInfoToUserTicketCertificateRequest(userId, request);
        APIResponse<TicketNftCardInformationVO> response = userSingleTicketCertificateManageApi
                .singleTicketCardInformation(APIRequest.instance(request));
        baseHelper.checkResponse(response);

        // i18n
        if (null != response.getData()) {
            fanTokenI18nHelper.doTicketNftCardInformationVO(response.getData());
        }

        // 增加 isTimeout 标识 Ticket 是否过期
        return new CommonRet<>(response.getData());
    }

    @PostMapping("/singleticket-card-rewards")
    public CommonRet<SingleTicketCardRewardVO> singleTicketCardBenefit(@RequestBody UserSingleTicketCertificateRequest request) {

        Long userId = baseHelper.getUserId();
        if (null == userId || null == request) {
            return new CommonRet<>();
        }

        fillingUserInfoToUserTicketCertificateRequest(userId, request);
        APIResponse<SingleTicketCardRewardVO> response = userSingleTicketCertificateManageApi
                .singleTicketCardBenefit(APIRequest.instance(request));
        baseHelper.checkResponse(response);

        // i18n
        if (null != response.getData()) {
            fanTokenI18nHelper.doSingleTicketCardRewardVO(response.getData());
        }

        return new CommonRet<>(response.getData());
    }

    @PostMapping("/singleticket-claim-rewards")
    public CommonRet<Void> singleTicketClaimRewards(@RequestBody UserSingleTicketCertificateRequest request) {

        Long userId = baseHelper.getUserId();
        if (null == userId || null == request) {
            return new CommonRet<>();
        }

        // 强制 KYC 的校验
        fanTokenCheckHelper.userComplianceValidate(userId);

        request.setIsGray(fanTokenCheckHelper.isGray());
        request.setUserId(userId);
        request.setClientType(baseHelper.getClientType());
        request.setLocale(baseHelper.getLanguage());
        request.setHasKyc(Boolean.TRUE);
        request.setComplianceAssetDto(fanTokenCheckHelper.fanTokenComplianceAsset(baseHelper.getUserId()));

        APIResponse<Void> response = userSingleTicketCertificateManageApi.singleTicketClaimRewards(APIRequest.instance(request));
        baseHelper.checkResponse(response);

        return new CommonRet<>();
    }
}
