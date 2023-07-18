package com.binance.mgs.nft.nftasset.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.binance.master.models.APIRequest;
import com.binance.master.models.APIResponse;
import com.binance.master.utils.CopyBeanUtils;
import com.binance.mgs.nft.market.helper.ArtistHelper;
import com.binance.mgs.nft.nftasset.controller.helper.ProductHelper;
import com.binance.mgs.nft.nftasset.controller.helper.UserHelper;
import com.binance.mgs.nft.nftasset.vo.*;
import com.binance.nft.assetservice.api.ICashBalanceApi;
import com.binance.nft.assetservice.api.INftAssetApi;
import com.binance.nft.assetservice.api.IUserInfoApi;
import com.binance.nft.assetservice.api.data.request.UserAccountRefreshRequest;
import com.binance.nft.assetservice.api.data.request.UserCashBalanceRequest;
import com.binance.nft.assetservice.api.data.response.BaseResponse;
import com.binance.nft.assetservice.api.data.response.UserTabResponse;
import com.binance.nft.assetservice.api.data.vo.*;
import com.binance.nft.assetservice.enums.NftAssetStatusEnum;
import com.binance.nft.tradeservice.api.ISellOrderApi;
import com.binance.nft.tradeservice.vo.NftProductInfoVo;
import com.binance.platform.mgs.base.helper.BaseHelper;
import com.binance.platform.mgs.base.vo.CommonRet;
import io.swagger.annotations.Api;
import lombok.RequiredArgsConstructor;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.math.NumberUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;

import javax.validation.constraints.NotNull;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

@Api
@RestController
@RequestMapping("/v1/private/nft")
@RequiredArgsConstructor
public class UserInfoController {

    private final IUserInfoApi userInfoApi;
    private final INftAssetApi nftAssetApi;
    private final ICashBalanceApi cashBalanceApi;

    private final BaseHelper baseHelper;
    private final ProductHelper productHelper;
    private final ArtistHelper artistHelper;
    private final UserHelper userHelper;
    private final ISellOrderApi sellOrderApi;


    @Value("${nft.pre.white-list.mint:}")
    private List<Long> whiteList;


    @GetMapping(value = "/user-info/simple-info")
    public CommonRet<UserSimpleInfoRet> getSimpleUserInfo() {

        Long userId = baseHelper.getUserId();
        if (null == userId){
            return new CommonRet<>();
        }
        APIResponse<UserSimpleInfoDto> userSimpleInfo = userInfoApi.fetchUserSimpleInfo(userId);
        baseHelper.checkResponse(userSimpleInfo);
        UserSimpleInfoDto data = userSimpleInfo.getData();

        UserSimpleInfoRet ret = null == data ? null : CopyBeanUtils.fastCopy(data, UserSimpleInfoRet.class);
        if(ret != null){
            ret.setArtist(true);
        }
        return new CommonRet<>(ret);
    }

    @GetMapping(value = "/user-nft-asset/{market-status}")
    public CommonRet<Page<NftAssetPersonalRet>> getUserNftAsset(@NotNull @RequestParam int page,
                                                               @NotNull @RequestParam int pageSize,
                                                               @PathVariable("market-status") Byte marketStatus){

        Long userId = baseHelper.getUserId();
        if (null == userId){
            return new CommonRet<>();
        }

        NftAssetConditionDto conditionDto = NftAssetConditionDto.builder()
                .userId(userId)
                .marketStatus(marketStatus)
                .build();
        APIResponse<Page<NftAssetPersonalVo>> pageAPIResponse = nftAssetApi
                .getUserNftAssetListByCondition(page, pageSize, APIRequest.instance(conditionDto));
        baseHelper.checkResponse(pageAPIResponse);
        Page<NftAssetPersonalVo> pageData = pageAPIResponse.getData();

        AtomicReference<Boolean> modifyTotalFlag = new AtomicReference<>(Boolean.FALSE);

        Page<NftAssetPersonalRet> retPage = new Page<>();
        retPage.setCurrent(pageData.getCurrent());
        retPage.setSize(pageData.getSize());
        retPage.setPages(pageData.getPages());

        List<NftAssetPersonalRet> retList = new ArrayList<>();
        if (NftAssetStatusEnum.MARKET_READY.getCode() == marketStatus){
            retList = productHelper.convertDataRegular(new ArrayList<>(), pageData);

        }else {

            List<NftProductInfoVo> infoRegular = productHelper.getProductInfoRegular(pageData, modifyTotalFlag);
            retList = productHelper.convertDataRegular(infoRegular, pageData);
        }
        retPage.setTotal(modifyTotalFlag.get() ? pageData.getTotal() - NumberUtils.LONG_ONE : pageData.getTotal());
        retPage.setRecords(retList);

        return new CommonRet<>(retPage);

    }

    @PostMapping("/user-asset-board")
    public CommonRet<UserCashBalanceVo> getUserAssetBoard(@RequestBody UserAssetBalanceArg request) {

        Long userId = baseHelper.getUserId();
        if (null == userId){
            return new CommonRet<>();
        }
        UserCashBalanceRequest userCashBalanceRequest = CopyBeanUtils.fastCopy(request, UserCashBalanceRequest.class);
        userCashBalanceRequest.setUserId(userId);

        APIResponse<UserCashBalanceVo> apiResponse = cashBalanceApi.getUserCashBalance(baseHelper.getInstance(userCashBalanceRequest));
        baseHelper.checkResponse(apiResponse);
        if(Objects.nonNull(apiResponse.getData()) && CollectionUtils.isNotEmpty(apiResponse.getData().getAssetBalanceList())) {
            String currencys = apiResponse.getData().getAssetBalanceList().stream()
                    .map(UserCashBalanceVo.AssetBalance::getAsset).collect(Collectors.joining(","));
            APIResponse<Map<String, Boolean>> unverifiedRsp = sellOrderApi.existUnverified(userId, currencys);
            Map<String, Boolean> unverifiedMap = Optional.ofNullable(unverifiedRsp.getData()).orElse(new HashMap<>());
            List<UserCashBalanceVo.AssetBalance> mabList = apiResponse.getData().getAssetBalanceList().stream().map(ab -> {
                MgsAssetBalance mab = CopyBeanUtils.fastCopy(ab, MgsAssetBalance.class);
                mab.setHasFrozen(unverifiedMap.getOrDefault(mab.getAsset(), false));
                return mab;
            }).collect(Collectors.toList());

            apiResponse.getData().setAssetBalanceList(mabList);
        }

        return new CommonRet<>(apiResponse.getData());

    }

    @PostMapping("/user-info/simple")
    public CommonRet<BaseResponse> addUserSimpleInfo(@RequestBody UserSimpleInfoDto request){

        Long userId = baseHelper.getUserId();
        if (null == userId){
            return new CommonRet<>();
        }

        return new CommonRet<>(userInfoApi.addUserSimpleInfo(APIRequest.instance(request)).getData());
    }

    @PostMapping("/user-info/personal/setting/simple")
    public CommonRet<BaseResponse> updateUserSimpleInfo(@RequestBody UserInfoSettingDto request){

        Long userId = baseHelper.getUserId();
        if (null == userId || Objects.isNull(request)){
            return new CommonRet<>();
        }
        request.setUserId(userId);

        APIResponse<BaseResponse> response = userInfoApi.updateUserSimpleInfo(APIRequest.instance(request));
        baseHelper.checkResponse(response);
        return new CommonRet<>(response.getData());
    }

    @GetMapping("/user-info/personal/open-tab")
    public CommonRet<UserTabResponse> getUserOpenTab(){

        Long userId = baseHelper.getUserId();
        if (null == userId){
            return new CommonRet<>();
        }
        return new CommonRet<>(userInfoApi.findOpenTabInfoByUserId(userId).getData());
    }

    @PutMapping("/user-info/personal/setting/simple")
    public CommonRet<BaseResponse> updateUserEmail(@RequestBody UserAccountRefreshArg arg){

        Long userId = baseHelper.getUserId();
        if (null == userId || Objects.isNull(arg)){
            return new CommonRet<>();
        }

        final UserAccountRefreshRequest request = UserAccountRefreshRequest.builder()
                .userId(userId).email(arg.getEmail())
                .build();

        return new CommonRet<>(userInfoApi.updateUserSimpleInfoFromFe(APIRequest.instance(request)).getData());
    }

    @GetMapping("/user-account/personal/simple-info")
    public CommonRet<UserSimpleAccountVo> fetchUserSimpleAccountInfo(){

        Long userId = baseHelper.getUserId();
        if (null == userId){
            return new CommonRet<>();
        }

        return new CommonRet<>(userHelper.fetchUserSimpleAccount(userId));
    }
}
