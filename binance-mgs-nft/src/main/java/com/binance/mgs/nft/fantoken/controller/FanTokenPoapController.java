package com.binance.mgs.nft.fantoken.controller;

import com.binance.master.models.APIRequest;
import com.binance.master.models.APIResponse;
import com.binance.mgs.nft.fantoken.helper.FanTokenCheckHelper;
import com.binance.mgs.nft.mysterybox.helper.FanTokenI18nHelper;
import com.binance.nft.fantoken.ifae.poap.IPoapManageApi;
import com.binance.nft.fantoken.profile.api.ifae.IFanTokenQRCodeApi;
import com.binance.nft.fantoken.profile.api.request.CreatePOAPBranchQRCodeRequest;
import com.binance.nft.fantoken.profile.api.request.CreatePOAPMainQRCodeRequest;
import com.binance.nft.fantoken.profile.api.response.CreateQRCodeResponse;
import com.binance.nft.fantoken.request.poap.PoapRequest;
import com.binance.nft.fantoken.vo.FanTokenNftItemInfo;
import com.binance.nft.fantoken.vo.poap.BranchVenueVO;
import com.binance.nft.fantoken.vo.poap.MainVenueVO;
import com.binance.platform.mgs.base.helper.BaseHelper;
import com.binance.platform.mgs.base.vo.CommonRet;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * <h1>FanToken Poap</h1>
 * */
@SuppressWarnings("all")
@Slf4j
@RequestMapping("/v1/friendly/nft/fantoken/poap/xyz")
@RestController
@RequiredArgsConstructor
public class FanTokenPoapController {

    private final BaseHelper baseHelper;
    private final FanTokenI18nHelper fanTokenI18nHelper;
    private final FanTokenCheckHelper fanTokenCheckHelper;
    private final IPoapManageApi poapManageApi;
    private final IFanTokenQRCodeApi fanTokenQRCodeApi;

    @PostMapping("/main-venue")
    public CommonRet<MainVenueVO> buildMainVenueInfo(@RequestBody PoapRequest request) {

        // 用户可以不登录, 所以, 不需要校验 userId 是否是 null
        if (null != request) {
            request.setIsGray(fanTokenCheckHelper.isGray());
            request.setUserId(baseHelper.getUserId());
            request.setClientType(baseHelper.getClientType());
            request.setLocale(baseHelper.getLanguage());
        }

        APIResponse<MainVenueVO> response = poapManageApi.buildMainVenueInfo(APIRequest.instance(request));
        baseHelper.checkResponse(response);

        // i18n
        if (null != response.getData()) {
            fanTokenI18nHelper.doMainVenueVO(response.getData());
        }

        return new CommonRet<>(response.getData());
    }

    @PostMapping("/branch-venue")
    public CommonRet<BranchVenueVO> buildBranchVenueInfo(@RequestBody PoapRequest request) {

        // 用户可以不登录, 所以, 不需要校验 userId 是否是 null
        if (null != request) {
            request.setIsGray(fanTokenCheckHelper.isGray());
            request.setUserId(baseHelper.getUserId());
            request.setClientType(baseHelper.getClientType());
            request.setLocale(baseHelper.getLanguage());
        }

        APIResponse<BranchVenueVO> response = poapManageApi.buildBranchVenueInfo(APIRequest.instance(request));
        baseHelper.checkResponse(response);

        // i18n
        if (null != response.getData()) {
            fanTokenI18nHelper.doBranchVenueVO(response.getData());
        }

        return new CommonRet<>(response.getData());
    }

    @PostMapping("/claim-branch-venue-nft")
    public CommonRet<FanTokenNftItemInfo> claimBranchVenueNft(@RequestBody PoapRequest request) {

        Long userId = baseHelper.getUserId();
        if (null == userId) {
            return new CommonRet<>();
        }

        if (null != request) {
            request.setIsGray(fanTokenCheckHelper.isGray());
            request.setUserId(baseHelper.getUserId());
            request.setClientType(baseHelper.getClientType());
            request.setLocale(baseHelper.getLanguage());
        }

        APIResponse<FanTokenNftItemInfo> response = poapManageApi.claimBranchVenueNft(APIRequest.instance(request));
        baseHelper.checkResponse(response);

        return new CommonRet<>(response.getData());
    }

    @PostMapping("/query-claim-branch-venue-nft")
    public CommonRet<FanTokenNftItemInfo> queryClaimBranchVenueNft(@RequestBody PoapRequest request) {

        Long userId = baseHelper.getUserId();
        if (null == userId) {
            return new CommonRet<>();
        }

        if (null != request) {
            request.setIsGray(fanTokenCheckHelper.isGray());
            request.setUserId(baseHelper.getUserId());
            request.setClientType(baseHelper.getClientType());
            request.setLocale(baseHelper.getLanguage());
        }

        APIResponse<FanTokenNftItemInfo> response = poapManageApi.queryClaimBranchVenueNft(APIRequest.instance(request));
        baseHelper.checkResponse(response);

        return new CommonRet<>(response.getData());
    }

    @PostMapping("/open-branch-venue-nft")
    public CommonRet<FanTokenNftItemInfo> openBranchVenueNft(@RequestBody PoapRequest request) {

        Long userId = baseHelper.getUserId();
        if (null == userId) {
            return new CommonRet<>();
        }

        if (null != request) {
            request.setIsGray(fanTokenCheckHelper.isGray());
            request.setUserId(baseHelper.getUserId());
            request.setClientType(baseHelper.getClientType());
            request.setLocale(baseHelper.getLanguage());
        }

        APIResponse<FanTokenNftItemInfo> response = poapManageApi.openBranchVenueNft(APIRequest.instance(request));
        baseHelper.checkResponse(response);

        return new CommonRet<>(response.getData());
    }

    @PostMapping("/query-open-branch-venue-nft")
    public CommonRet<FanTokenNftItemInfo> queryOpenBranchVenueNft(@RequestBody PoapRequest request) {

        Long userId = baseHelper.getUserId();
        if (null == userId) {
            return new CommonRet<>();
        }

        if (null != request) {
            request.setIsGray(fanTokenCheckHelper.isGray());
            request.setUserId(baseHelper.getUserId());
            request.setClientType(baseHelper.getClientType());
            request.setLocale(baseHelper.getLanguage());
        }

        APIResponse<FanTokenNftItemInfo> response = poapManageApi.queryOpenBranchVenueNft(APIRequest.instance(request));
        baseHelper.checkResponse(response);

        return new CommonRet<>(response.getData());
    }

    @PostMapping("/composite-main-venue-nft")
    public CommonRet<FanTokenNftItemInfo> compositeMainVenueNft(@RequestBody PoapRequest request) {

        Long userId = baseHelper.getUserId();
        if (null == userId) {
            return new CommonRet<>();
        }

        // 强制 KYC 的校验
        fanTokenCheckHelper.userComplianceValidate(userId);

        if (null != request) {
            request.setIsGray(fanTokenCheckHelper.isGray());
            request.setUserId(baseHelper.getUserId());
            request.setClientType(baseHelper.getClientType());
            request.setLocale(baseHelper.getLanguage());
        }

        APIResponse<FanTokenNftItemInfo> response = poapManageApi.compositeMainVenueNft(APIRequest.instance(request));
        baseHelper.checkResponse(response);

        return new CommonRet<>(response.getData());
    }

    @PostMapping("/query-composite-main-venue-nft")
    public CommonRet<FanTokenNftItemInfo> queryCompositeMainVenueNft(@RequestBody PoapRequest request) {

        Long userId = baseHelper.getUserId();
        if (null == userId) {
            return new CommonRet<>();
        }

        // 强制 KYC 的校验
        fanTokenCheckHelper.userComplianceValidate(userId);

        if (null != request) {
            request.setIsGray(fanTokenCheckHelper.isGray());
            request.setUserId(baseHelper.getUserId());
            request.setClientType(baseHelper.getClientType());
            request.setLocale(baseHelper.getLanguage());
        }

        APIResponse<FanTokenNftItemInfo> response = poapManageApi.queryCompositeMainVenueNft(APIRequest.instance(request));
        baseHelper.checkResponse(response);

        return new CommonRet<>(response.getData());
    }

    @PostMapping("/open-main-venue-nft")
    public CommonRet<FanTokenNftItemInfo> openMainVenueNft(@RequestBody PoapRequest request) {

        Long userId = baseHelper.getUserId();
        if (null == userId) {
            return new CommonRet<>();
        }

        // 强制 KYC 的校验
        fanTokenCheckHelper.userComplianceValidate(userId);

        if (null != request) {
            request.setIsGray(fanTokenCheckHelper.isGray());
            request.setUserId(baseHelper.getUserId());
            request.setClientType(baseHelper.getClientType());
            request.setLocale(baseHelper.getLanguage());
        }

        APIResponse<FanTokenNftItemInfo> response = poapManageApi.openMainVenueNft(APIRequest.instance(request));
        baseHelper.checkResponse(response);

        return new CommonRet<>(response.getData());
    }

    @PostMapping("/query-open-main-venue-nft")
    public CommonRet<FanTokenNftItemInfo> queryOpenMainVenueNft(@RequestBody PoapRequest request) {

        Long userId = baseHelper.getUserId();
        if (null == userId) {
            return new CommonRet<>();
        }

        // 强制 KYC 的校验
        fanTokenCheckHelper.userComplianceValidate(userId);

        if (null != request) {
            request.setIsGray(fanTokenCheckHelper.isGray());
            request.setUserId(baseHelper.getUserId());
            request.setClientType(baseHelper.getClientType());
            request.setLocale(baseHelper.getLanguage());
        }

        APIResponse<FanTokenNftItemInfo> response = poapManageApi.queryOpenMainVenueNft(APIRequest.instance(request));
        baseHelper.checkResponse(response);

        return new CommonRet<>(response.getData());
    }

    @PostMapping("/compensation-branch-venue")
    public CommonRet<BranchVenueVO> buildCompensationBranchVenueInfo(@RequestBody PoapRequest request) {

        // 用户可以不登录, 所以, 不需要校验 userId 是否是 null
        if (null != request) {
            request.setIsGray(fanTokenCheckHelper.isGray());
            request.setUserId(baseHelper.getUserId());
            request.setClientType(baseHelper.getClientType());
            request.setLocale(baseHelper.getLanguage());
        }

        APIResponse<BranchVenueVO> response = poapManageApi.buildCompensationBranchVenueInfo(APIRequest.instance(request));
        baseHelper.checkResponse(response);

        // i18n
        if (null != response.getData()) {
            fanTokenI18nHelper.doBranchVenueVO(response.getData());
        }

        return new CommonRet<>(response.getData());
    }

    @PostMapping("/query-open-main-venue-qrcode")
    public CommonRet<CreateQRCodeResponse> queryOpenMainVenueQrcode(@RequestBody CreatePOAPMainQRCodeRequest request) {

        String campaignId = request.getCampaignId();

        if (null == campaignId) {
            return new CommonRet<>();
        }
        // check campaign
        PoapRequest poapRequest = new PoapRequest();
        poapRequest.setCampaignId(campaignId);
        poapRequest.setIsGray(fanTokenCheckHelper.isGray());
        poapRequest.setUserId(baseHelper.getUserId());
        poapRequest.setClientType(baseHelper.getClientType());
        poapRequest.setLocale(baseHelper.getLanguage());
        APIResponse<MainVenueVO> poapResponse = poapManageApi.buildMainVenueInfo(APIRequest.instance(poapRequest));
        baseHelper.checkResponse(poapResponse);
        if (null == poapResponse.getData() || null == poapResponse.getData().getTeamInfo()) {
            return new CommonRet<>();
        }
        // set symbol
        request.setSymbol(poapResponse.getData().getTeamInfo().getSymbol());

        APIResponse<CreateQRCodeResponse> response = fanTokenQRCodeApi.createPOAPMainQRCode(APIRequest.instance(request));
        baseHelper.checkResponse(response);

        return new CommonRet<>(response.getData());
    }

    @PostMapping("/query-open-branch-venue-qrcode")
    public CommonRet<CreateQRCodeResponse> queryOpenBranchVenueQrcode(@RequestBody CreatePOAPBranchQRCodeRequest request) {

        String campaignPieceId = request.getCampaignPieceId();

        if (null == campaignPieceId) {
            return new CommonRet<>();
        }
        // check campaign
        PoapRequest poapRequest = new PoapRequest();
        poapRequest.setCampaignPieceId(campaignPieceId);
        poapRequest.setIsGray(fanTokenCheckHelper.isGray());
        poapRequest.setUserId(baseHelper.getUserId());
        poapRequest.setClientType(baseHelper.getClientType());
        poapRequest.setLocale(baseHelper.getLanguage());
        APIResponse<BranchVenueVO> poapResponse = poapManageApi.buildBranchVenueInfo(APIRequest.instance(poapRequest));
        baseHelper.checkResponse(poapResponse);
        if (null == poapResponse.getData() || null == poapResponse.getData().getTeamInfo()) {
            return new CommonRet<>();
        }
        // set symbol
        request.setSymbol(poapResponse.getData().getTeamInfo().getSymbol());

        APIResponse<CreateQRCodeResponse> response = fanTokenQRCodeApi.createPOAPBranchQRCode(APIRequest.instance(request));
        baseHelper.checkResponse(response);

        return new CommonRet<>(response.getData());
    }
}
