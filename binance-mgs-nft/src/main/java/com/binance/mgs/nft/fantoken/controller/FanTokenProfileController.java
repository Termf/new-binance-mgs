package com.binance.mgs.nft.fantoken.controller;

import com.alibaba.fastjson.JSON;
import com.binance.master.error.BusinessException;
import com.binance.master.error.GeneralCode;
import com.binance.master.models.APIRequest;
import com.binance.master.models.APIResponse;
import com.binance.master.utils.StringUtils;
import com.binance.mgs.nft.fantoken.helper.FanTokenCheckHelper;
import com.binance.mgs.nft.mysterybox.helper.FanTokenI18nHelper;
import com.binance.nft.fantoken.ifae.IFanProfileManageApi;
import com.binance.nft.fantoken.profile.api.ifae.FanProfileUserProfileAPI;
import com.binance.nft.fantoken.profile.api.ifae.IFanProfileActionCookieApi;
import com.binance.nft.fantoken.profile.api.ifae.IFanProfileTagApi;
import com.binance.nft.fantoken.profile.api.ifae.IFanProfileUserSettingApi;
import com.binance.nft.fantoken.profile.api.ifae.IFanTokenQRCodeApi;
import com.binance.nft.fantoken.profile.api.request.ActionCookieRequest;
import com.binance.nft.fantoken.profile.api.request.CommonProfileRequest;
import com.binance.nft.fantoken.profile.api.request.CreateQRCodeRequest;
import com.binance.nft.fantoken.profile.api.request.QueryUserProfileRequest;
import com.binance.nft.fantoken.profile.api.request.SaveUserSettingRequest;
import com.binance.nft.fantoken.profile.api.request.SaveUserTagsRequest;
import com.binance.nft.fantoken.profile.api.request.ShareNftRequest;
import com.binance.nft.fantoken.profile.api.request.UpdateUserProfileBannerRequest;
import com.binance.nft.fantoken.profile.api.response.CreateQRCodeResponse;
import com.binance.nft.fantoken.profile.api.response.QueryUserTagsResponse;
import com.binance.nft.fantoken.profile.api.response.UserProfileQueryResponse;
import com.binance.nft.fantoken.profile.api.vo.ActionCookieVO;
import com.binance.nft.fantoken.profile.api.vo.FanProfileBannerVO;
import com.binance.nft.fantoken.profile.api.vo.SimpleNftAssetInfo;
import com.binance.nft.fantoken.request.CommonPageRequest;
import com.binance.nft.fantoken.response.CommonPageResponse;
import com.binance.nft.fantoken.vo.fanprofile.FanCollectionVO;
import com.binance.nft.fantoken.vo.fanprofile.FanProfileBannerDetailVO;
import com.binance.platform.mgs.base.helper.BaseHelper;
import com.binance.platform.mgs.base.vo.CommonRet;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Objects;

/**
 * <h1>FanProfile Controller</h1>
 * */
@Slf4j
@RequestMapping("/v1")
@RestController
@RequiredArgsConstructor
public class FanTokenProfileController {

    /** 基础工具 */
    private final BaseHelper baseHelper;
    private final FanTokenI18nHelper fanTokenI18nHelper;
    private final FanTokenCheckHelper fanTokenCheckHelper;

    /** fanprofile 微服务中定义 */
    private final FanProfileUserProfileAPI fanProfileUserProfileAPI;
    private final IFanProfileTagApi fanProfileTagApi;
    private final IFanProfileUserSettingApi fanProfileUserSettingApi;
    private final IFanProfileActionCookieApi fanProfileActionCookieApi;
    private final IFanTokenQRCodeApi fanTokenQRCodeApi;

    /** fantoken 微服务中定义 */
    private final IFanProfileManageApi fanProfileManageApi;

    @Value("${fanprofile.user.check.nickname.limit:20}")
    private Integer nicknameLengthLimit;

    @PostMapping("/friendly/nft/fantoken/profile/user")
    public CommonRet<UserProfileQueryResponse> queryUserProfile(@RequestBody QueryUserProfileRequest request) {

        try {
            Long userId = baseHelper.getUserId();
            String ownerNickname = request.getOwnerNickname();
            // 校验昵称长度超过主站限制
            if (StringUtils.length(ownerNickname) > nicknameLengthLimit) {
                return new CommonRet<>();
            }
            request.setCurrentUserId(userId);
            APIResponse<UserProfileQueryResponse> response = fanProfileUserProfileAPI.queryUserProfile(APIRequest.instance(request));

            // 如果没有返回用户数据, 代表接口查询出错, 直接抛异常
            if (null == response || null == response.getData()) {
                log.error("illegal user profile request: [req={}]", request.getOwnerNickname());
                throw new BusinessException(GeneralCode.SYS_ERROR);
            }

            baseHelper.checkResponse(response);
            // i18n
            fanTokenI18nHelper.doFanprofileTagInfos(response.getData().getTagInfos());

            return new CommonRet<>(response.getData());
        } catch (Exception ex) {
            log.warn("query user profile has some ex: [req={}], [ex={}]", JSON.toJSONString(request), ex.getMessage(), ex);
        }

        return new CommonRet<>();
    }

    /**
     * 保存用户Tag，FanProfile2.0 前端删除
     */
    @PostMapping("/private/nft/fantoken/profile/save-user-tags")
    public CommonRet<Void> saveUserTags(@RequestBody SaveUserTagsRequest request) {

        Long userId = baseHelper.getUserId();
        if (null == userId) {
            return new CommonRet<>();
        }
        request.setUserId(userId);
        APIResponse<Void> response = fanProfileTagApi.saveUserTags(APIRequest.instance(request));
        baseHelper.checkResponse(response);

        return new CommonRet<>();
    }

    @GetMapping("/private/nft/fantoken/profile/query-fanprofile-tags")
    public CommonRet<QueryUserTagsResponse> queryUserFanprofileTags() {

        Long userId = baseHelper.getUserId();
        if (null == userId) {
            return new CommonRet<>();
        }
        APIResponse<QueryUserTagsResponse> response = fanProfileTagApi.queryUserFanprofileTags(APIRequest.instance(userId));
        baseHelper.checkResponse(response);

        // i18n
        fanTokenI18nHelper.doFanprofileTagInfos(response.getData().getTagInfos());

        return new CommonRet<>(response.getData());
    }

    @PostMapping("/private/nft/fantoken/profile/save-user-settings")
    public CommonRet<Void> saveUserSetting(@RequestBody SaveUserSettingRequest request) {

        Long userId = baseHelper.getUserId();
        if (null == userId) {
            return new CommonRet<>();
        }
        request.setUserId(userId);
        APIResponse<Void> response = fanProfileUserSettingApi.saveUserSetting(APIRequest.instance(request));
        baseHelper.checkResponse(response);

        return new CommonRet<>();
    }

    @PostMapping("/friendly/nft/fantoken/action/query-cookie")
    public CommonRet<ActionCookieVO> queryActionCookie(@RequestBody ActionCookieRequest request) {

        Long userId = baseHelper.getUserId();
        if (null == userId) {
            return new CommonRet<>();
        }

        // 对请求参数做简单校验, 参数不合法不发送请求到微服务
        if (CollectionUtils.isEmpty(request.getBannerUrls())) {
            return new CommonRet<>();
        }

        request.setUserId(userId);
        request.setIsGray(fanTokenCheckHelper.isGray());

        APIResponse<ActionCookieVO> response = fanProfileActionCookieApi.queryActionCookie(APIRequest.instance(request));
        baseHelper.checkResponse(response);

        return new CommonRet<>(response.getData());
    }

    @PostMapping("/friendly/nft/fantoken/action/save-cookie")
    public CommonRet<Void> saveActionCookie(@RequestBody ActionCookieRequest request) {

        Long userId = baseHelper.getUserId();
        if (null == userId) {
            return new CommonRet<>();
        }

        request.setUserId(userId);
        request.setIsGray(fanTokenCheckHelper.isGray());

        APIResponse<Void> response = fanProfileManageApi.saveActionCookie(APIRequest.instance(request));
        baseHelper.checkResponse(response);

        return new CommonRet<>();
    }

    @PostMapping("/private/nft/fantoken/profile/query-user-collections")
    public CommonRet<CommonPageResponse<SimpleNftAssetInfo>> userHoldNftAssetInfo(@RequestBody CommonPageRequest<
            CommonProfileRequest> request) {

        Long userId = baseHelper.getUserId();
        if (null == userId) {
            return new CommonRet<>();
        }

        // 强制 KYC 的校验
        fanTokenCheckHelper.userComplianceValidate(userId);

        request.getParams().setIsGray(fanTokenCheckHelper.isGray());
        request.getParams().setUserId(baseHelper.getUserId());
        request.getParams().setClientType(baseHelper.getClientType());

        APIResponse<CommonPageResponse<SimpleNftAssetInfo>> response = fanProfileManageApi.userHoldNftAssetInfo(
                APIRequest.instance(request));
        baseHelper.checkResponse(response);

        return new CommonRet<>(response.getData());
    }

    @PostMapping("/private/nft/fantoken/profile/save-user-share-collections")
    public CommonRet<Void> userSelectNftForShare(@RequestBody ShareNftRequest request) {

        Long userId = baseHelper.getUserId();
        if (null == userId || Objects.isNull(request.getSelectedCollectionInfos())) {
            return new CommonRet<>();
        }

        // 强制 KYC 的校验
        fanTokenCheckHelper.userComplianceValidate(userId);

        request.setIsGray(fanTokenCheckHelper.isGray());
        request.setUserId(baseHelper.getUserId());

        APIResponse<Void> response = fanProfileManageApi.userSelectNftForShare(APIRequest.instance(request));
        baseHelper.checkResponse(response);

        return new CommonRet<>();
    }

    @PostMapping("/private/nft/fantoken/profile/query-user-shared-nft")
    public CommonRet<List<SimpleNftAssetInfo>> queryUserSharedNft() {

        Long userId = baseHelper.getUserId();
        if (null == userId) {
            return new CommonRet<>();
        }

        CommonProfileRequest request = CommonProfileRequest.builder()
                .userId(baseHelper.getUserId())
                .isGray(fanTokenCheckHelper.isGray())
                .clientType(baseHelper.getClientType())
                .build();

        APIResponse<List<SimpleNftAssetInfo>> response = fanProfileManageApi.queryUserSharedNft(APIRequest.instance(request));
        baseHelper.checkResponse(response);

        return new CommonRet<>(response.getData());
    }

    @PostMapping("/friendly/nft/fantoken/profile/query-user-banner")
    public CommonRet<FanProfileBannerVO> queryUserProfileBanner(@RequestBody CommonProfileRequest request) {

        request.setIsGray(fanTokenCheckHelper.isGray());
        request.setUserId(baseHelper.getUserId());
        request.setClientType(baseHelper.getClientType());

        APIResponse<FanProfileBannerVO> response = fanProfileManageApi.queryUserProfileBanner(APIRequest.instance(request));
        baseHelper.checkResponse(response);

        return new CommonRet<>(response.getData());
    }

    @PostMapping("/private/nft/fantoken/profile/save-user-banner")
    public CommonRet<Void> updateUserProfileBanner(@RequestBody UpdateUserProfileBannerRequest request) {

        Long userId = baseHelper.getUserId();
        if (null == userId) {
            return new CommonRet<>();
        }

        request.setIsGray(fanTokenCheckHelper.isGray());
        request.setUserId(baseHelper.getUserId());

        APIResponse<Void> response = fanProfileManageApi.updateUserProfileBanner(APIRequest.instance(request));
        baseHelper.checkResponse(response);

        return new CommonRet<>();
    }

    @PostMapping("/private/nft/fantoken/profile/query-fanprofile-banners")
    public CommonRet<CommonPageResponse<FanProfileBannerDetailVO>> queryFanProfileBanner(@RequestBody CommonPageRequest<
            CommonProfileRequest> request) {

        Long userId = baseHelper.getUserId();
        if (null == userId) {
            return new CommonRet<>();
        }

        request.getParams().setIsGray(fanTokenCheckHelper.isGray());
        request.getParams().setUserId(baseHelper.getUserId());
        request.getParams().setClientType(baseHelper.getClientType());

        APIResponse<CommonPageResponse<FanProfileBannerDetailVO>> response = fanProfileManageApi.queryFanProfileBanner(
                APIRequest.instance(request));
        baseHelper.checkResponse(response);

        return new CommonRet<>(response.getData());
    }

    @PostMapping("/friendly/nft/fantoken/profile/display-fan-collections")
    public CommonRet<CommonPageResponse<FanCollectionVO>> displayFanCollection(@RequestBody CommonPageRequest<
            CommonProfileRequest> request) {

        Long userId = baseHelper.getUserId();
        if (null == userId && StringUtils.isBlank(request.getParams().getOwnerNickname())) {
            return new CommonRet<>();
        }

        request.getParams().setIsGray(fanTokenCheckHelper.isGray());
        request.getParams().setUserId(baseHelper.getUserId());
        request.getParams().setClientType(baseHelper.getClientType());

        APIResponse<CommonPageResponse<FanCollectionVO>> response = fanProfileManageApi.displayFanCollection(
                APIRequest.instance(request));
        baseHelper.checkResponse(response);

        // i18n
        if (null != response.getData() && CollectionUtils.isNotEmpty(response.getData().getData())) {
            fanTokenI18nHelper.doDisplayFanCollection(response.getData().getData());
        }

        return new CommonRet<>(response.getData());
    }

    @PostMapping("/private/nft/fantoken/profile/share-nft-create-qrcode")
    public CommonRet<CreateQRCodeResponse> createUserShareNftQRCode() {

        Long userId = baseHelper.getUserId();
        if (null == userId) {
            return new CommonRet<>();
        }

        CreateQRCodeRequest request = CreateQRCodeRequest.builder()
                .userId(userId)
                .local(baseHelper.getLanguage())
                .build();

        APIResponse<CreateQRCodeResponse> response = fanTokenQRCodeApi.createUserShareNftQRCode(APIRequest.instance(request));
        baseHelper.checkResponse(response);

        return new CommonRet<>(response.getData());
    }
}
