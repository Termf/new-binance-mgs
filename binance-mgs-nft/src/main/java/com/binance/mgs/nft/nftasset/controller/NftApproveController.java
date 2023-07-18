package com.binance.mgs.nft.nftasset.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.binance.master.error.BusinessException;
import com.binance.master.models.APIRequest;
import com.binance.master.models.APIResponse;
import com.binance.master.utils.CopyBeanUtils;
import com.binance.mgs.nft.google.GoogleRecaptha;
import com.binance.mgs.nft.market.utils.AesUtil;
import com.binance.mgs.nft.nftasset.controller.helper.ProductHelper;
import com.binance.mgs.nft.nftasset.vo.*;
import com.binance.nft.assetservice.api.IUserApproveApi;
import com.binance.nft.assetservice.api.data.request.UserApproveQueryRequest;
import com.binance.nft.assetservice.api.data.request.UserApproveRequest;
import com.binance.nft.assetservice.api.data.response.NftApproveInfoDto;
import com.binance.nft.assetservice.api.data.vo.ApproveUserListVo;
import com.binance.nft.assetservice.constant.NftAssetErrorCode;
import com.binance.nft.assetservice.enums.NftSourceEnum;
import com.binance.nft.common.utils.ObjectUtils;
import com.binance.nft.tradeservice.response.ProductDetailResponse;
import com.binance.nft.tradeservice.response.ProductDetailV2Response;
import com.binance.nft.tradeservice.vo.ProductDetailVo;
import com.binance.platform.mgs.annotations.UserOperation;
import com.binance.platform.mgs.base.helper.BaseHelper;
import com.binance.platform.mgs.base.vo.CommonRet;
import io.swagger.annotations.Api;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Objects;

@Api
@Slf4j
@RestController
@RequestMapping("/v1/private/nft")
@RequiredArgsConstructor
public class NftApproveController {

    private final IUserApproveApi userApproveApi;

    private final ProductHelper productHelper;

    private final BaseHelper baseHelper;

    @Value("${nft.aes.password}")
    private String AES_PASSWORD;

    @UserOperation(eventName = "NFT_Approve", name = "NFT_Approve",
            requestKeys = {"productId","status"},
            requestKeyDisplayNames = {"productId","status"},
            responseKeys = {"$.code", "$.message", "$.errorMessage", "$.errorCode"},
            responseKeyDisplayNames = {"code", "message", "errorMessage", "errorCode"})
    @GoogleRecaptha(value = "/private/nft/user-approve/approve",message = "Robots are not allowed to like NFT")
    @PostMapping("/user-approve/approve")
    public CommonRet<Boolean> approve(@RequestBody UserApproveVo userApproveVo) {

        Long userId = baseHelper.getUserId();
        if (null == userId) {
            return new CommonRet<>();
        }
        ProductDetailResponse productDetailResponse = productHelper
                .queryProductDetailById(userId, userApproveVo.getProductId());
        ProductDetailVo productDetail = productDetailResponse.getProductDetail();
        if(null != productDetail){
            if(!Objects.equals(productDetail.getStatus(), 1) && userApproveVo.getStatus() > 0){
                //只有上架的商品才能点赞
                throw new BusinessException(NftAssetErrorCode.USER_APPROVE_ERROR);
            }
        }
        UserApproveRequest request = UserApproveRequest.builder()
                .status(userApproveVo.getStatus())
                .productId(userApproveVo.getProductId())
                .setEndTime(productDetail != null ?
                        productDetailResponse.getProductDetail().getSetEndTime().getTime() : null)
                .sellerId(productDetailResponse.getProductDetail().getCreatorId())
                .nftInfoId(productDetailResponse.getNftInfo() != null ?
                        productDetailResponse.getNftInfo().getNftId() : 0L)
                .userId(userId).build();
        APIResponse<Void> voidAPIResponse = userApproveApi.userApprove(APIRequest.instance(request));
        baseHelper.checkResponse(voidAPIResponse);
        return new CommonRet<>(true);
    }

    @UserOperation(eventName = "NFT_Approve", name = "NFT_Approve",
            requestKeys = {"nftInfoId","status"},
            requestKeyDisplayNames = {"nftInfoId","status"},
            responseKeys = {"$.code", "$.message", "$.errorMessage", "$.errorCode"},
            responseKeyDisplayNames = {"code", "message", "errorMessage", "errorCode"})
    @GoogleRecaptha(value = "/private/nft/user-approve/approve",message = "Robots are not allowed to like NFT")
    @PostMapping("/user-approve/approve-nft-id")
    public CommonRet<Boolean> approveByNftId(@RequestBody UserApproveNftInfoVo userApproveNftInfoVo) {

        Long userId = baseHelper.getUserId();
        if (Objects.isNull(userId)) {
            return new CommonRet<>();
        }
        if(Objects.isNull(userApproveNftInfoVo.getNftInfoId())) {
            throw new BusinessException(NftAssetErrorCode.USER_APPROVE_ERROR);
        }
        UserApproveRequest request = UserApproveRequest.builder()
                .status(userApproveNftInfoVo.getStatus())
                .nftInfoId(userApproveNftInfoVo.getNftInfoId())
                .userId(userId).build();
        ProductDetailV2Response productDetailV2Response = productHelper.getProductDetailV2Response(userId, userApproveNftInfoVo.getNftInfoId());
        if(Objects.nonNull(productDetailV2Response) && Objects.equals(productDetailV2Response.getProductDetail().getStatus(), 1)) {
            //只要上架的商品才需要添加商品id
            request.setProductId(productDetailV2Response.getProductDetail().getId());
            request.setSellerId(productDetailV2Response.getProductDetail().getCreatorId());
            request.setSetEndTime(productDetailV2Response.getProductDetail().getSetEndTime().getTime());
        }
        APIResponse<Void> voidAPIResponse = userApproveApi.userApprove(APIRequest.instance(request));
        baseHelper.checkResponse(voidAPIResponse);
        return new CommonRet<>(true);
    }

    @UserOperation(eventName = "NFT_Approve", name = "NFT_Approve",
            requestKeys = {"nftInfoId","status"},
            requestKeyDisplayNames = {"nftInfoId","status"},
            responseKeys = {"$.code", "$.message", "$.errorMessage", "$.errorCode"},
            responseKeyDisplayNames = {"code", "message", "errorMessage", "errorCode"})
    @PostMapping("/user-approve/nft-approve")
    public CommonRet<Boolean> nftApprove(@RequestBody UserNftApproveVo userNftApproveVo) {

        Long userId = baseHelper.getUserId();
        if (null == userId) {
            return new CommonRet<>();
        }
        UserApproveRequest request = UserApproveRequest.builder()
                .status(userNftApproveVo.getStatus())
                .nftInfoId(userNftApproveVo.getNftInfoId())
                .userId(userId).build();
        APIResponse<Void> voidAPIResponse = userApproveApi.userApprove(APIRequest.instance(request));
        baseHelper.checkResponse(voidAPIResponse);
        return new CommonRet<>(true);
    }

    @GetMapping("/user-approve/list")
    public CommonRet<Page<ProductItemWithApproveMgs>> list(@RequestParam("current") int current,
                                                        @RequestParam("size") int size) {

        Long userId = baseHelper.getUserId();
        if (null == userId) {
            return new CommonRet<>();
        }

        UserApproveQueryRequest request = UserApproveQueryRequest.builder()
                .userId(userId)
                .current(current)
                .pageSize(size)
                .build();
        APIResponse<Page<ApproveUserListVo>> response = userApproveApi.listApprove(APIRequest.instance(request));
        baseHelper.checkResponse(response);

        Page<ApproveUserListVo> data = response.getData();
        List<ProductItemWithApprove> responseList = productHelper.queryProductItemVo(data.getRecords());

        List<ProductItemWithApproveMgs> resp = new ArrayList<>(responseList.size());
        responseList.forEach(item -> {
            ProductItemWithApproveMgs mgs = CopyBeanUtils.fastCopy(item, ProductItemWithApproveMgs.class);

            ArtistUserInfoMgs artistUserInfoMgs = null;

            if (!ObjectUtils.isEmpty(item.getCreator())){
                artistUserInfoMgs = CopyBeanUtils.fastCopy(item.getCreator(), ArtistUserInfoMgs.class);
                if (item.getCreator().getUserId() != null){
                    artistUserInfoMgs.setUserId(AesUtil.encrypt(item.getCreator().getUserId().toString(), AES_PASSWORD));
                }
            }
            mgs.setCreator(artistUserInfoMgs);
            mgs.setTimestamp(new Date());
            resp.add(mgs);
        });

        Page<ProductItemWithApproveMgs> result = new Page();
        result.setCurrent(data.getCurrent());
        result.setTotal(data.getTotal());
        result.setSize(data.getSize());
        result.setRecords(resp);

        return new CommonRet<>(result);
    }

    @GetMapping("/user-approve/de-list")
    public CommonRet<Page<NftApproveInfoDto>> deList(@RequestParam("current") int current,
                                              @RequestParam("size") int size) {

        Long userId = baseHelper.getUserId();
        if (null == userId) {
            return new CommonRet<>();
        }

        UserApproveQueryRequest request = UserApproveQueryRequest.builder()
                .userId(userId)
                .current(current)
                .pageSize(size)
                .build();

        APIResponse<Page<NftApproveInfoDto>> response = userApproveApi.deListApprove(APIRequest.instance(request));
        for(NftApproveInfoDto nftApproveInfoDto : response.getData().getRecords()) {
            if(nftApproveInfoDto != null && NftSourceEnum.DEPOSIT.getDescription().equals(nftApproveInfoDto.getNftSource())) {
                nftApproveInfoDto.setCreator(null);
            }
        }

        return new CommonRet(response.getData());
    }
}
