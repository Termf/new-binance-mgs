package com.binance.mgs.nft.nftasset.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.binance.master.error.BusinessException;
import com.binance.master.utils.StringUtils;
import com.binance.mgs.nft.market.utils.AesUtil;
import com.binance.mgs.nft.nftasset.controller.helper.ActivityCR7Helper;
import com.binance.mgs.nft.nftasset.controller.helper.NftAssetHelper;
import com.binance.mgs.nft.nftasset.vo.NftProfileAssetVo;
import com.binance.mgs.nft.nftasset.vo.NftProfileCollectionVo;
import com.binance.nft.assetservice.api.data.request.GetUserCollectionsRequest;
import com.binance.nft.assetservice.api.data.request.UserProfileRequest;
import com.binance.nft.market.vo.MarketSuggestionList;
import com.binance.nft.tradeservice.enums.TradeErrorCode;
import com.binance.platform.mgs.base.helper.BaseHelper;
import com.binance.platform.mgs.base.vo.CommonRet;
import io.swagger.annotations.Api;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang.math.NumberUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Objects;

@Api
@RequestMapping("/v1/friendly/nft/profile")
@RestController
@RequiredArgsConstructor
public class NftProfileAssetController {

    private final NftAssetHelper nftAssetHelper;
    private final ActivityCR7Helper activityCR7Helper;
    private final BaseHelper baseHelper;

    @Value("${nft.aes.password}")
    private String AES_PASSWORD;

    @PostMapping("/user/asset/collections")
    public CommonRet<Page<NftProfileCollectionVo>> fetchUserProfileAssetCollections(@RequestBody UserProfileRequest request, HttpServletRequest httpServletRequest) throws Exception {
        request.setUserId(null);
        request.setType(1);
        return fetchUserProfileCollections(request, isGray(httpServletRequest));
    }

    @PostMapping("/user/create/collections")
    public CommonRet<Page<NftProfileCollectionVo>> fetchUserProfileCreateCollections(@RequestBody UserProfileRequest request, HttpServletRequest httpServletRequest) throws Exception{
        request.setUserId(null);
        request.setType(2);
        return fetchUserProfileCollections(request, isGray(httpServletRequest));
    }

    @PostMapping("/user/approve/collections")
    public CommonRet<Page<NftProfileCollectionVo>> fetchUserProfileApproveCollections(@RequestBody UserProfileRequest request, HttpServletRequest httpServletRequest) throws Exception {
        request.setUserId(null);
        request.setType(3);
        return fetchUserProfileCollections(request, isGray(httpServletRequest));
    }

    @PostMapping("/user/collect/asset")
    public CommonRet<Page<NftProfileAssetVo>> fetchUserProfileCollectionAssetVo(@RequestBody UserProfileRequest request) throws Exception {
        request.setUserId(null);
        request.setType(1);
        return fetchUserProfileAssetVo(request);
    }

    @PostMapping("/user/create/asset")
    public CommonRet<Page<NftProfileAssetVo>> fetchUserProfileCreateAssetVo(@RequestBody UserProfileRequest request) throws Exception {
        request.setUserId(null);
        request.setType(2);
        return fetchUserProfileAssetVo(request);
    }

    @PostMapping("/user/approve/asset")
    public CommonRet<Page<NftProfileAssetVo>> fetchUserProfileApproveAssetVo(@RequestBody UserProfileRequest request) throws Exception {
        request.setUserId(null);
        request.setType(3);
        return fetchUserProfileAssetVo(request);
    }

    private CommonRet<Page<NftProfileCollectionVo>> fetchUserProfileCollections(UserProfileRequest request, boolean isGray) throws Exception {
        Long userId = baseHelper.getUserId();
        if(Objects.isNull(userId) && StringUtils.isEmpty(request.getProfileStrId())) {
            throw new BusinessException(TradeErrorCode.PARAM_ERROR);
        } else {
            if (StringUtils.isNotEmpty(request.getProfileStrId())) {
                Long profileId = null;
                if(StringUtils.isNotEmpty(request.getProfileStrId())) {
                    if (NumberUtils.isDigits(request.getProfileStrId())){
                        profileId = Long.parseLong(request.getProfileStrId());
                    }else{
                        try {
                            profileId = Long.parseLong(AesUtil.decrypt(request.getProfileStrId(), AES_PASSWORD));
                        }catch (Exception e){
                            throw new BusinessException(TradeErrorCode.PARAM_ERROR);
                        }
                    }
                }
                if(Objects.isNull(userId)) {
                    //陌生人访问B
                    request.setIsOwner(false);
                    request.setProfileId(profileId);
                    return new CommonRet<>(nftAssetHelper.fetchUserProfileCollections(request, isGray));
                } else {
                    //A登录
                    if(userId.equals(profileId)) {
                        //访问A
                        request.setIsOwner(true);
                        request.setProfileId(profileId);
                        request.setUserId(userId);
                        return new CommonRet<>(nftAssetHelper.fetchUserProfileCollections(request, isGray));
                    } else {
                        //访问B
                        request.setIsOwner(false);
                        request.setProfileId(profileId);
                        request.setUserId(userId);
                        return new CommonRet<>(nftAssetHelper.fetchUserProfileCollections(request, isGray));
                    }
                }
            } else {
                //A登录，访问自己
                request.setIsOwner(true);
                request.setProfileId(userId);
                request.setUserId(userId);
                return new CommonRet<>(nftAssetHelper.fetchUserProfileCollections(request, isGray));
            }
        }
    }

    public CommonRet<Page<NftProfileAssetVo>> fetchUserProfileAssetVo(UserProfileRequest request) throws Exception {
        Long userId = baseHelper.getUserId();
        if(Objects.isNull(userId) && StringUtils.isEmpty(request.getProfileStrId())) {
            throw new BusinessException(TradeErrorCode.PARAM_ERROR);
        } else {
            if (StringUtils.isNotEmpty(request.getProfileStrId())) {
                Long profileId = null;
                if(StringUtils.isNotEmpty(request.getProfileStrId())) {
                    if (NumberUtils.isDigits(request.getProfileStrId())){
                        profileId = Long.parseLong(request.getProfileStrId());
                    }else{
                        try {
                            profileId = Long.parseLong(AesUtil.decrypt(request.getProfileStrId(), AES_PASSWORD));
                        }catch (Exception e){
                            throw new BusinessException(TradeErrorCode.PARAM_ERROR);
                        }
                    }
                }
                if(Objects.isNull(userId)) {
                    //陌生人访问B
                    request.setIsOwner(false);
                    request.setProfileId(profileId);
                    return new CommonRet<>(nftAssetHelper.fetchUserProfileAssets(request));
                } else {
                    //A登录
                    if(userId.equals(profileId)) {
                        //访问A
                        request.setIsOwner(true);
                        request.setProfileId(profileId);
                        request.setUserId(userId);
                        return new CommonRet<>(nftAssetHelper.fetchUserProfileAssets(request));
                    } else {
                        //访问B
                        request.setIsOwner(false);
                        request.setProfileId(profileId);
                        request.setUserId(userId);
                        return new CommonRet<>(nftAssetHelper.fetchUserProfileAssets(request));
                    }
                }
            } else {
                //A登录，访问自己
                request.setIsOwner(true);
                request.setProfileId(userId);
                request.setUserId(userId);
                return new CommonRet<>(nftAssetHelper.fetchUserProfileAssets(request));
            }
        }
    }

    public Boolean isGray(HttpServletRequest request) {
        String envFlag = request.getHeader("x-gray-env");
        return org.apache.commons.lang3.StringUtils.isNotBlank(envFlag) && !"normal".equals(envFlag);
    }
}
