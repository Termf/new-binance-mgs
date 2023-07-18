package com.binance.mgs.nft.nftasset.controller;

import com.binance.master.error.BusinessException;
import com.binance.master.models.APIRequest;
import com.binance.master.models.APIResponse;
import com.binance.master.utils.CopyBeanUtils;
import com.binance.master.utils.IPUtils;
import com.binance.master.utils.StringUtils;
import com.binance.master.utils.WebUtils;
import com.binance.mgs.nft.market.helper.ArtistHelper;
import com.binance.mgs.nft.market.utils.AesUtil;
import com.binance.mgs.nft.nftasset.controller.helper.ActivityCR7Helper;
import com.binance.mgs.nft.nftasset.controller.helper.ProfileHelper;
import com.binance.mgs.nft.nftasset.vo.UserProfileArg;
import com.binance.mgs.nft.nftasset.vo.UserProfileInfoRet;
import com.binance.nft.activityservice.response.CR7ClaimableResponse;
import com.binance.nft.assetservice.api.IUserInfoApi;
import com.binance.nft.assetservice.api.data.request.follow.FollowProfileReq;
import com.binance.nft.assetservice.api.data.vo.UserSimpleInfoDto;
import com.binance.nft.assetservice.api.data.vo.follow.UserFollowCountVo;
import com.binance.nft.assetservice.api.follow.IUserFollowApi;
import com.binance.nft.bnbgtwservice.api.data.dto.UserComplianceCheckRet;
import com.binance.nft.bnbgtwservice.api.data.req.UserComplianceCheckReq;
import com.binance.nft.bnbgtwservice.api.iface.IUserComplianceApi;
import com.binance.nft.bnbgtwservice.common.enums.ComplianceTypeEnum;
import com.binance.nft.tradeservice.enums.TradeErrorCode;
import com.binance.platform.mgs.base.helper.BaseHelper;
import com.binance.platform.mgs.base.vo.CommonRet;
import io.swagger.annotations.Api;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang.math.NumberUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.Objects;

@Api
@RestController
@RequestMapping("/v1/friendly/nft/profile")
@RequiredArgsConstructor
public class UserProfileController {

    private final IUserInfoApi userInfoApi;
    private final BaseHelper baseHelper;
    private final ArtistHelper artistHelper;
    private final ActivityCR7Helper cr7Helper;
    private final IUserComplianceApi iUserComplianceApi;

    private final IUserFollowApi userFollowApi;
    private final ProfileHelper profileHelper;

    @Value("${nft.aes.password}")
    private String AES_PASSWORD;


    @PostMapping("/user-info")
    public CommonRet<UserProfileInfoRet> getUserProfile(@RequestBody UserProfileArg request) {
        //login in userId
        Long userId = baseHelper.getUserId();
        UserProfileInfoRet ret = null;
        String profileStrId = null;
        if(Objects.nonNull(request) && Objects.nonNull(request.getProfileStrId())) {
            profileStrId = request.getProfileStrId();
        }
        if(Objects.isNull(userId) && StringUtils.isEmpty(profileStrId)) {
            throw new BusinessException(TradeErrorCode.PARAM_ERROR);
        } else {
            if(StringUtils.isNotEmpty(profileStrId)) {
                Long profileId = null;
                if(StringUtils.isNotEmpty(profileStrId)) {
                    if (NumberUtils.isDigits(profileStrId)){
                        profileId = Long.parseLong(profileStrId);
                    }else{
                        try {
                            profileId = Long.parseLong(AesUtil.decrypt(profileStrId, AES_PASSWORD));
                        }catch (Exception e){
                            throw new BusinessException(TradeErrorCode.PARAM_ERROR);
                        }
                    }
                }
                APIResponse<UserSimpleInfoDto> userSimpleInfo = userInfoApi.fetchUserSimpleInfo(profileId);

                APIResponse<UserFollowCountVo> userFollowInfo = userFollowApi.findUserFollowInfo(APIRequest.instance(
                        FollowProfileReq.builder().userId(profileId).ownerId(userId).build()));
                baseHelper.checkResponse(userFollowInfo);
                ret = getUserProfileInfoRet(userSimpleInfo, userFollowInfo.getData());
                ret.setArtist(true);
                APIResponse<UserComplianceCheckRet> response = iUserComplianceApi.complianceCheck(APIRequest.instance(generateReq(profileId)));
                baseHelper.checkResponse(response);
                ret.setKyc(checkKyc(profileId));
                if(Objects.isNull(userId)) {
                    //陌生人访问B
                    ret.setUserId(null);
                    ret.setOwner(false);
                } else {
                    //A登录访问B
                    if(userId.equals(profileId)) {
                        ret.setUserId(AesUtil.encrypt(userId.toString(), AES_PASSWORD));
                        ret.setOwner(true);
                        cr7Helper.checkCR7Claimable(userId, ret);
                    } else {
                        ret.setUserId(AesUtil.encrypt(userId.toString(), AES_PASSWORD));
                        ret.setOwner(false);
                    }
                }
                ret.setTotalVolume(profileHelper.getProfileVolume(profileId));
            } else {
                //A登录，访问自己
                APIResponse<UserSimpleInfoDto> userSimpleInfo = userInfoApi.fetchUserSimpleInfo(userId);

                APIResponse<UserFollowCountVo> userFollowInfo = userFollowApi.findUserFollowInfo(APIRequest.instance(
                        FollowProfileReq.builder().userId(userId).ownerId(userId).build()));
                baseHelper.checkResponse(userFollowInfo);
                ret = getUserProfileInfoRet(userSimpleInfo, userFollowInfo.getData());
                ret.setOwner(true);
                ret.setArtist(true);
                ret.setKyc(checkKyc(userId));
                ret.setUserId(AesUtil.encrypt(userId.toString(), AES_PASSWORD));
                //是否有待领取的kyc盲盒
                cr7Helper.checkCR7Claimable(userId, ret);
                ret.setTotalVolume(profileHelper.getProfileVolume(userId));
            }
        }
        ret.setEmail(null);
        return new CommonRet<>(ret);
    }

    private UserProfileInfoRet getUserProfileInfoRet(APIResponse<UserSimpleInfoDto> userSimpleInfo,
                                                     UserFollowCountVo userFollowCountVo) {
        baseHelper.checkResponse(userSimpleInfo);
        UserSimpleInfoDto data = userSimpleInfo.getData();
        UserProfileInfoRet ret = null == data ? null : CopyBeanUtils.fastCopy(data, UserProfileInfoRet.class);
        ret.setUserId(null);
        ret.setMintCount(data.getMintCount() != null ? data.getMintCount() : 0);
        if(userFollowCountVo != null) {
            ret.setFollowing(userFollowCountVo.getFollowCount() != null ? userFollowCountVo.getFollowCount() : 0);
            ret.setFans(userFollowCountVo.getFansCount() != null ? userFollowCountVo.getFansCount() : 0);
            ret.setFollow(userFollowCountVo.isFollow());
        }
        return ret;
    }

    private boolean checkKyc(Long userId) {
        APIResponse<UserComplianceCheckRet> response = iUserComplianceApi
                .complianceCheck(APIRequest.instance(generateReq(userId)));
        baseHelper.checkResponse(response);
        return response.getData().getPass();
    }

    private UserComplianceCheckReq generateReq(Long userId) {
        return UserComplianceCheckReq.builder()
                .type(ComplianceTypeEnum.genType(ComplianceTypeEnum.KYC_CHECK, ComplianceTypeEnum.CLEAR_CHECK))
                .userId(userId)
                .ip(IPUtils.getIpAddress(WebUtils.getHttpServletRequest()))
                .front(false)
                .build();
    }

}
