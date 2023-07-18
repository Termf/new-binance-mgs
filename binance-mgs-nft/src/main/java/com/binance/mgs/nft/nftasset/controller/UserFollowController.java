package com.binance.mgs.nft.nftasset.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.binance.master.error.BusinessException;
import com.binance.master.models.APIRequest;
import com.binance.master.models.APIResponse;
import com.binance.mgs.nft.google.GoogleRecaptha;
import com.binance.mgs.nft.market.utils.AesUtil;
import com.binance.mgs.nft.nftasset.controller.helper.KycHelper;
import com.binance.mgs.nft.nftasset.vo.FollowReq;
import com.binance.nft.assetservice.api.data.request.follow.CreateFollowReq;
import com.binance.nft.assetservice.api.data.request.follow.FollowListQuery;
import com.binance.nft.assetservice.api.data.response.follow.FollowItem;
import com.binance.nft.assetservice.api.follow.IUserFollowApi;
import com.binance.nft.assetservice.constant.NftAssetErrorCode;
import com.binance.nft.assetservice.enums.FollowTypeEnum;
import com.binance.platform.mgs.annotations.UserOperation;
import com.binance.platform.mgs.base.helper.BaseHelper;
import com.binance.platform.mgs.base.vo.CommonRet;
import lombok.RequiredArgsConstructor;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/v1")
@RequiredArgsConstructor
public class UserFollowController {

    private final IUserFollowApi userFollowApi;

    private final BaseHelper baseHelper;

    private final KycHelper kycHelper;

    @Value("${nft.aes.password}")
    private String AES_PASSWORD;

    @GetMapping("/friendly/nft/follow/follow-list")
    public CommonRet<Page<FollowItem>> followList(@RequestParam("visitor") String visitor,
                                                  @RequestParam(value = "current") Integer current,
                                                  @RequestParam(value = "pageSize") Integer pageSize) {


        FollowListQuery listQuery = FollowListQuery.builder()
                .useId(Long.valueOf(AesUtil.decrypt(visitor, AES_PASSWORD)))
                .type(FollowTypeEnum.FOLLOW.getType())
                .pageSize(pageSize)
                .current(current)
                .build();
        Long userId = baseHelper.getUserId();
        if(userId != null) {
            listQuery.setOwnerId(userId);
        }
        APIResponse<Page<FollowItem>> response = userFollowApi.followingList(APIRequest.instance(listQuery));

        baseHelper.checkResponse(response);
        Page<FollowItem> data = response.getData();
        data.setRecords(convert(data.getRecords()));
        return new CommonRet<>(data);
    }

    @GetMapping("/friendly/nft/follow/follows")
    public CommonRet<Page<FollowItem>> follows(@RequestParam("visitor") String visitor,
                                      @RequestParam(value = "current") Integer current,
                                      @RequestParam(value = "pageSize") Integer pageSize) {


        FollowListQuery listQuery = FollowListQuery.builder()
                .useId(Long.valueOf(AesUtil.decrypt(visitor, AES_PASSWORD)))
                .type(FollowTypeEnum.FANS.getType())
                .pageSize(pageSize)
                .current(current)
                .build();
        Long userId = baseHelper.getUserId();
        if(userId != null) {
            listQuery.setOwnerId(userId);
        }
        APIResponse<Page<FollowItem>> response = userFollowApi.followsList(APIRequest.instance(listQuery));

        baseHelper.checkResponse(response);
        Page<FollowItem> data = response.getData();
        data.setRecords(convert(data.getRecords()));
        return new CommonRet<>(data);
    }

    @PostMapping("/private/nft/follow/follow")
    @UserOperation(eventName = "NFT_User_Follow", name = "NFT_User_Follow", sendToBigData = true, sendToDb = true,
            responseKeys = {"$.code","$.message","$.data", "$.errorMessage", "$.errorCode"},
            responseKeyDisplayNames = {"code","message","data","errorMessage","errorCode"})
    @GoogleRecaptha(value = "/private/nft/follow/follow",message = "Robots are not allowed to follow user")
    public CommonRet<Boolean> follow(@RequestBody FollowReq followReq) {

        Long userId = baseHelper.getUserId();
        if(StringUtils.isBlank(followReq.getEncryptUserId()) || userId == null){
            return new CommonRet<>(false);
        }
        kycHelper.userComplianceValidate(userId);

        Long targetUserId = Long.valueOf(AesUtil.decrypt(followReq.getEncryptUserId(), AES_PASSWORD));

        if(targetUserId == userId) {
            throw new BusinessException(NftAssetErrorCode.BAD_REQUEST);
        }

        CreateFollowReq req = new CreateFollowReq();
        req.setUserId(userId);
        req.setFollowId(targetUserId);

        APIResponse<Boolean> response = userFollowApi.createFollow(APIRequest.instance(req));
        baseHelper.checkResponse(response);
        return new CommonRet<>(response.getData());
    }

    @PostMapping("/private/nft/follow/un-follow")
    @UserOperation(eventName = "NFT_User_UnFollow", name = "NFT_User_UnFollow", sendToBigData = true, sendToDb = true,
            responseKeys = {"$.code","$.message","$.data", "$.errorMessage", "$.errorCode"},
            responseKeyDisplayNames = {"code","message","data","errorMessage","errorCode"})
    public CommonRet<Boolean> unFollow(@RequestBody FollowReq followReq) {

        Long userId = baseHelper.getUserId();
        if(StringUtils.isBlank(followReq.getEncryptUserId()) || userId == null){
            return new CommonRet<>(false);
        }
        Long targetUserId = Long.valueOf(AesUtil.decrypt(followReq.getEncryptUserId(), AES_PASSWORD));

        if(targetUserId == userId) {
            throw new BusinessException(NftAssetErrorCode.BAD_REQUEST);
        }

        CreateFollowReq req = new CreateFollowReq();
        req.setUserId(userId);
        req.setFollowId(targetUserId);

        APIResponse<Boolean> response = userFollowApi.removeFollow(APIRequest.instance(req));
        baseHelper.checkResponse(response);
        return new CommonRet<>(response.getData());
    }

    private List<FollowItem> convert(List<FollowItem> req) {

        if(CollectionUtils.isEmpty(req)){
            return Collections.emptyList();
        }

        return req.stream().map(item -> {
            item.setEncryptUserId(AesUtil.encrypt(String.valueOf(item.getUserId()), AES_PASSWORD));
            item.setUserId(null);
            return item;
        }).collect(Collectors.toList());
    }
}
