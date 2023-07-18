package com.binance.mgs.nft.nftasset.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.binance.master.error.BusinessException;
import com.binance.master.utils.CopyBeanUtils;
import com.binance.mgs.nft.market.utils.AesUtil;
import com.binance.mgs.nft.nftasset.controller.helper.NftLoggerHelper;
import com.binance.mgs.nft.nftasset.response.UserSimpleInfoResponse;
import com.binance.mgs.nft.nftasset.vo.NftEventVoRet;
import com.binance.nft.assetservice.api.data.vo.UserSimpleInfoDto;
import com.binance.nft.assetservice.constant.NftAssetErrorCode;
import com.binance.platform.mgs.base.vo.CommonRet;
import io.swagger.annotations.Api;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Objects;

@Api
@RequestMapping("/v1/public/nft/nft-info")
@RestController
@RequiredArgsConstructor
public class NftInfoController {

    private final NftLoggerHelper nftLoggerHelper;

    @Value("${nft.aes.password}")
    private String AES_PASSWORD;

    @GetMapping("/event/simple/{nft-info-id}")
    public CommonRet<Page<NftEventVoRet>> fetchNftEventSimplePageable(@RequestParam("page") int page,
                                                                      @RequestParam("pageSize") int pageSize,
                                                                      @RequestParam(name = "salesOnlyFlag", required = false)boolean salesOnlyFlag,
                                                                      @PathVariable("nft-info-id") Long nftInfoId) {

        if(Objects.isNull(nftInfoId)){
            throw new BusinessException(NftAssetErrorCode.SYSTEM_INTERNAL_ERROR);
        }
        return new CommonRet<>(
                nftLoggerHelper.fetchNftSimpleEventPage(page, pageSize, salesOnlyFlag, nftInfoId));
    }

    @GetMapping("/event/simple/owner/{nft-info-id}")
    public CommonRet<UserSimpleInfoResponse> fetchUserOwner(@PathVariable("nft-info-id") Long nftInfoId) {

        if(Objects.isNull(nftInfoId)){
            throw new BusinessException(NftAssetErrorCode.SYSTEM_INTERNAL_ERROR);
        }
        UserSimpleInfoDto userSimpleInfoDto = nftLoggerHelper.fetchUserOwner(nftInfoId);
        if (null == userSimpleInfoDto) {
            return new CommonRet<>();
        }
        final UserSimpleInfoResponse userSimpleInfoRet = CopyBeanUtils.fastCopy(userSimpleInfoDto, UserSimpleInfoResponse.class);
        userSimpleInfoRet.setUserId(AesUtil.encrypt(userSimpleInfoDto.getUserId().toString(), AES_PASSWORD));

        return new CommonRet<>(userSimpleInfoRet);
    }


}
