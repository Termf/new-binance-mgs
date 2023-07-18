package com.binance.mgs.nft.fantoken.controller;

import com.binance.master.models.APIRequest;
import com.binance.master.models.APIResponse;
import com.binance.master.utils.StringUtils;
import com.binance.mgs.nft.fantoken.helper.FanTokenCheckHelper;
import com.binance.mgs.nft.mysterybox.helper.FanTokenI18nHelper;
import com.binance.nft.fantoken.ifae.ticketnft.IUserTicketCertificateManageApi;
import com.binance.nft.fantoken.request.CommonPageRequest;
import com.binance.nft.fantoken.request.ticketnft.TicketNftEventCardDetailPageRequest;
import com.binance.nft.fantoken.request.ticketnft.UserTicketCertificateRequest;
import com.binance.nft.fantoken.response.ticketnft.TicketNftEventCardDetailPageResponse;
import com.binance.nft.fantoken.response.ticketnft.UserTicketCertificateResponse;
import com.binance.nft.fantoken.vo.ticketnft.TicketNftCardBenefitVO;
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
public class FanTokenTicketNftController {

    private final BaseHelper baseHelper;
    private final FanTokenI18nHelper fanTokenI18nHelper;
    private final FanTokenCheckHelper fanTokenCheckHelper;
    private final IUserTicketCertificateManageApi userTicketCertificateManageApi;

    /**
     * <h2>给 UserTicketCertificateRequest 填充登录用户信息</h2>
     * */
    private void fillingUserInfoToUserTicketCertificateRequest(Long userId, UserTicketCertificateRequest request) {

        request.setIsGray(fanTokenCheckHelper.isGray());
        request.setUserId(userId);
        request.setClientType(baseHelper.getClientType());
        request.setLocale(baseHelper.getLanguage());
        request.setHasKyc(null != userId && fanTokenCheckHelper.hasKyc(userId));
    }

    /**
     * <h2>用户扫描或输入卡号</h2>
     * 需要登录, 不需要 kyc
     * */
    @PostMapping("/submit-ticket-number")
    public CommonRet<UserTicketCertificateResponse> submitTicketCertificate(@RequestBody UserTicketCertificateRequest request) {

        Long userId = baseHelper.getUserId();
        if (null == userId || null == request) {
            return new CommonRet<>();
        }

        fillingUserInfoToUserTicketCertificateRequest(userId, request);
        request.setComplianceAssetDto(fanTokenCheckHelper.fanTokenComplianceAsset(baseHelper.getUserId()));
        APIResponse<UserTicketCertificateResponse> response = userTicketCertificateManageApi.submitTicketCertificate(
                APIRequest.instance(request));
        baseHelper.checkResponse(response);

        return new CommonRet<>(response.getData());
    }

    /**
     * <h2>轮询 NFT 分发结果信息 (500ms 一次, 最多三次)</h2>
     * 需要登录, 不需要 kyc
     * */
    @PostMapping("/ticketnft-query-nft-distribution")
    public CommonRet<UserTicketCertificateResponse> queryTicketNftDistribution(@RequestBody UserTicketCertificateRequest request) {

        Long userId = baseHelper.getUserId();
        if (null == userId || null == request) {
            return new CommonRet<>();
        }

        fillingUserInfoToUserTicketCertificateRequest(userId, request);
        APIResponse<UserTicketCertificateResponse> response = userTicketCertificateManageApi.queryTicketNftDistribution(
                APIRequest.instance(request));
        baseHelper.checkResponse(response);

        return new CommonRet<>(response.getData());
    }

    /**
     * <h2>ticket nft card detail</h2>
     * 需要登录, 不需要 kyc
     * */
    @PostMapping("/ticketnft-card-detail")
    public CommonRet<TicketNftCardDetailVO> ticketNftCardDetail(@RequestBody UserTicketCertificateRequest request) {

        Long userId = baseHelper.getUserId();
        if (null == userId || null == request) {
            return new CommonRet<>();
        }

        fillingUserInfoToUserTicketCertificateRequest(userId, request);
        APIResponse<TicketNftCardDetailVO> response = userTicketCertificateManageApi.ticketNftCardDetail(
                APIRequest.instance(request));
        baseHelper.checkResponse(response);

        // i18n
        if (null != response.getData()) {
            fanTokenI18nHelper.doTicketNftCardDetailVO(response.getData());
        }

        return new CommonRet<>(response.getData());
    }

    /**
     * <h2>ticket nft card details</h2>
     * 需要登录, 不需要 kyc
     * */
    @PostMapping("/ticketnft-card-details")
    public CommonRet<TicketNftEventCardDetailPageResponse> ticketNftCardDetails(@RequestBody CommonPageRequest<
            TicketNftEventCardDetailPageRequest> request) {

        Long userId = baseHelper.getUserId();
        if (null == userId
                || null == request || null == request.getParams() || null == request.getParams().getQuery()) {
            return new CommonRet<>();
        }

        Preconditions.checkArgument(StringUtils.isNotBlank(request.getParams().getQuery().getTeamId()),
                "teamId is invalid");

        // 填充 userId
        request.getParams().getQuery().setUserId(userId);

        APIResponse<TicketNftEventCardDetailPageResponse> response = userTicketCertificateManageApi.ticketNftCardDetails(
                APIRequest.instance(request));
        baseHelper.checkResponse(response);

        // i18n
        if (null != response.getData()) {
            fanTokenI18nHelper.doTicketNftEventCardDetailPageResponse(response.getData());
        }

        return new CommonRet<>(response.getData());
    }

    /**
     * <h2>ticket nft card information</h2>
     * 需要登录, 不需要 kyc
     * */
    @PostMapping("/ticketnft-card-information")
    public CommonRet<TicketNftCardInformationVO> ticketNftCardInformation(@RequestBody UserTicketCertificateRequest request) {

        Long userId = baseHelper.getUserId();
        if (null == userId || null == request) {
            return new CommonRet<>();
        }

        fillingUserInfoToUserTicketCertificateRequest(userId, request);
        APIResponse<TicketNftCardInformationVO> response = userTicketCertificateManageApi.ticketNftCardInformation(
                APIRequest.instance(request));
        baseHelper.checkResponse(response);

        // 增加 isTimeout 标识 Ticket 是否过期
        return new CommonRet<>(response.getData());
    }

    /**
     * <h2>ticket nft card benefits</h2>
     * 需要登录, 不需要 kyc
     * */
    @PostMapping("/ticketnft-card-benefits")
    public CommonRet<TicketNftCardBenefitVO> ticketNftCardBenefit(@RequestBody UserTicketCertificateRequest request) {

        Long userId = baseHelper.getUserId();
        if (null == userId || null == request) {
            return new CommonRet<>();
        }

        fillingUserInfoToUserTicketCertificateRequest(userId, request);
        APIResponse<TicketNftCardBenefitVO> response = userTicketCertificateManageApi.ticketNftCardBenefit(
                APIRequest.instance(request));
        baseHelper.checkResponse(response);

        // i18n
        if (null != response.getData()) {
            fanTokenI18nHelper.doTicketNftCardBenefitVO(response.getData());
        }

        return new CommonRet<>(response.getData());
    }

    /**
     * <h2>ticket nft card benefits</h2>
     * 需要登录, 需要 kyc
     * */
    @PostMapping("/ticketnft-claim-rewards")
    public CommonRet<Void> ticketNftClaimRewards(@RequestBody UserTicketCertificateRequest request) {

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

        APIResponse<Void> response = userTicketCertificateManageApi.ticketNftClaimRewards(APIRequest.instance(request));
        baseHelper.checkResponse(response);

        return new CommonRet<>();
    }

    /**
     * <h2>ticket nft event config</h2>
     * */
    @PostMapping("/ticketnft-event-config")
    public CommonRet<TicketNftCardDetailVO> ticketNftEventConfig(@RequestBody UserTicketCertificateRequest request) {

        fillingUserInfoToUserTicketCertificateRequest(baseHelper.getUserId(), request);
        APIResponse<TicketNftCardDetailVO> response = userTicketCertificateManageApi.ticketNftEventConfig(APIRequest.instance(request));
        baseHelper.checkResponse(response);

        // i18n
        if (null != response.getData()) {
            fanTokenI18nHelper.doTicketNftCardDetailVO(response.getData());
        }

        return new CommonRet<>(response.getData());
    }
}
