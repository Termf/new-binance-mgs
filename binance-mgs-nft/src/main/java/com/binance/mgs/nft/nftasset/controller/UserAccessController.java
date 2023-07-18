package com.binance.mgs.nft.nftasset.controller;


import com.binance.master.models.APIRequest;
import com.binance.master.models.APIResponse;
import com.binance.master.utils.WebUtils;
import com.binance.mgs.nft.nftasset.controller.helper.UserHelper;
import com.binance.mgs.nft.nftasset.vo.UserAccessDto;
import com.binance.mgs.nft.nftasset.vo.UserLimitCountDto;
import com.binance.nft.assetservice.api.data.vo.MinterInfo;
import com.binance.nft.assetservice.api.mintmanager.IMintManagerApi;
import com.binance.platform.mgs.base.helper.BaseHelper;
import com.binance.platform.mgs.base.vo.CommonRet;
import io.swagger.annotations.Api;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.math.NumberUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Objects;
import java.util.Optional;

@Api
@RestController
@RequestMapping("/v1/private/nft")
@RequiredArgsConstructor
@Slf4j
public class UserAccessController {

    private final BaseHelper baseHelper;
    private final UserHelper userHelper;

    private final IMintManagerApi mintManagerApi;

    @GetMapping("/user-info/white-list")
    public CommonRet<UserAccessDto> checkUserAccess(){

        Long userId = this.baseHelper.getUserId();

        log.info("user-id :: {}", WebUtils.getHeader("X-USER-ID"));
        log.info("user-email :: {}", WebUtils.getHeader("X-USER-EMAIL"));


        if (Objects.isNull(userId)){
            return new CommonRet<>(new UserAccessDto());
        }

        return new CommonRet<>(userHelper.checkUserWhiteList(userId));

    }

    @GetMapping("/user-info/limit-count")
    public CommonRet<UserLimitCountDto> getLimitCount(){
        Long userId = this.baseHelper.getUserId();
        if (Objects.isNull(userId)){
            //默认返回c等级
            return new CommonRet<>(new UserLimitCountDto());
        }
        APIResponse<MinterInfo> minterInfoAPIResponse = mintManagerApi.checkUserMintable(APIRequest.instance(String.valueOf(userId)));
        if(!baseHelper.isOk(minterInfoAPIResponse) ||Objects.isNull(minterInfoAPIResponse.getData())) {
            return new CommonRet<>(UserLimitCountDto.builder().build());
        }
        MinterInfo minterInfo = minterInfoAPIResponse.getData();
        int totalMintCount = Optional.ofNullable(minterInfo.getTotalMintCount()).orElse(0);
        int mintedCount = Optional.ofNullable(minterInfo.getMintedCount()).orElse(0);
        return new CommonRet<>(UserLimitCountDto.builder().limitMintCount(totalMintCount)
                .remainMintCount(NumberUtils.INTEGER_MINUS_ONE.equals(totalMintCount)
                        ?totalMintCount
                        :(totalMintCount-mintedCount))
                .build());
    }




}
