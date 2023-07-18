package com.binance.mgs.nft.nftasset.controller;

import com.binance.master.error.BusinessException;
import com.binance.master.models.APIRequest;
import com.binance.master.models.APIResponse;
import com.binance.master.utils.CopyBeanUtils;
import com.binance.mgs.nft.nftasset.vo.UserAmountLimitDto;
import com.binance.mgs.nft.nftasset.vo.UserAmountLimitEditDto;
import com.binance.mgs.nft.nftasset.vo.UserQuotaEditDto;
import com.binance.mgs.nft.trade.config.TradeConfig;
import com.binance.nft.assetservice.api.IUserQuotaApi;
import com.binance.nft.assetservice.api.data.dto.UserQuotaItemDto;
import com.binance.nft.assetservice.api.data.request.Security2faDto;
import com.binance.nft.assetservice.api.data.request.UserQuotaCheckRequest;
import com.binance.nft.assetservice.api.data.response.UserQuotaDetailResp;
import com.binance.nft.assetservice.constant.NftAssetErrorCode;
import com.binance.nft.bnbgtwservice.api.data.dto.AccountLimitEditRequest;
import com.binance.nft.bnbgtwservice.api.data.dto.AccountLimitQueryRequest;
import com.binance.nft.bnbgtwservice.api.data.dto.LimitDetailDto;
import com.binance.nft.bnbgtwservice.api.data.dto.SecurityDto;
import com.binance.nft.bnbgtwservice.api.data.dto.accountlimit.AccountLimitQueryResponse;
import com.binance.nft.bnbgtwservice.api.iface.IAccountLimitApi;
import com.binance.nft.bnbgtwservice.api.iface.ISecurity2faApi;
import com.binance.platform.mgs.base.helper.BaseHelper;
import com.binance.platform.mgs.base.vo.CommonRet;
import com.ctrip.framework.apollo.core.utils.StringUtils;
import io.swagger.annotations.Api;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Api
@Slf4j
@RestController
@RequestMapping("/v1/private/nft")
@RequiredArgsConstructor
public class NftQuotaController {

    private final IUserQuotaApi userQuotaApi;

    private final BaseHelper baseHelper;

    private final IAccountLimitApi accountLimitApi;

    private final ISecurity2faApi security2faApi;

    private final TradeConfig tradeConfig;

    @Value("${verify.free.purchase.biztype:VerifyFreePurchase}")
    private String verifyFreeBizType;

    @PostMapping("/quota/user-quota/edit")
    public CommonRet<Boolean> edit(@RequestBody UserQuotaEditDto userQuotaEditDto) {

        Long userId = baseHelper.getUserId();
        if (null == userId) {
            return new CommonRet<>();
        }
        UserQuotaItemDto userQuotaItemDto = userQuotaEditDto.getUserQuotaItemDto();
        userQuotaItemDto.setUserId(userId);
        userQuotaItemDto.setCurrency("USD");
        UserQuotaCheckRequest userQuotaCheckRequest = new UserQuotaCheckRequest();
        userQuotaCheckRequest.setUserQuotaItemDto(userQuotaItemDto);
        userQuotaCheckRequest.setSecurity2faDto(userQuotaEditDto.getSecurity2faDto());
        APIResponse<Boolean> response = userQuotaApi.modifyUserQuotaInfo(
                APIRequest.instance(userQuotaCheckRequest));
        baseHelper.checkResponse(response);
        return new CommonRet<>(response.getData());
    }

    @GetMapping("/quota/user-quota/query")
    public CommonRet<UserQuotaDetailResp> query() {

        Long userId = baseHelper.getUserId();
        if (null == userId) {
            return new CommonRet<>();
        }
        APIResponse<UserQuotaDetailResp> resp = userQuotaApi.findUserQuotaDetailByUserId(APIRequest.instance(userId));
        baseHelper.checkResponse(resp);
        return new CommonRet<>(replaceUserQuotaDetails(resp.getData()));
    }

    @GetMapping("/quota/user-quota/queryFreePurchase")
    public CommonRet<UserQuotaItemDto> queryFreePurchase() {

        Long userId = baseHelper.getUserId();
        if (null == userId) {
            return new CommonRet<>();
        }
        UserQuotaItemDto purchase = doQueryAccountLimit(userId,verifyFreeBizType);
        return new CommonRet<>(purchase);
    }

    private UserQuotaDetailResp replaceUserQuotaDetails(UserQuotaDetailResp resp) {
        UserQuotaItemDto purchase = doQueryAccountLimit(resp.getUserId(),"Purchase");
        if(purchase != null) {
            purchase.setType("purchase");

        }
        UserQuotaItemDto verifyFreePurchase = doQueryAccountLimit(resp.getUserId(), verifyFreeBizType);
        if(verifyFreePurchase != null) {
            verifyFreePurchase.setType("verify_free_purchase");
        }
        if(verifyFreePurchase != null) {
            UserAmountLimitDto dto = CopyBeanUtils.fastCopy(verifyFreePurchase,UserAmountLimitDto.class);
            dto.setMinAmount(BigDecimal.ZERO);
            dto.setMaxAmount(tradeConfig.getMaxAccountLimitAmount());
            verifyFreePurchase = (UserQuotaItemDto)dto;
        }


        List<UserQuotaItemDto> listing = resp.getItemList().stream().filter(item -> item.getType().equals("listing")).collect(Collectors.toList());
        listing.add(purchase);
        listing.add(verifyFreePurchase);
        resp.setItemList(listing);
        listing.stream().forEach(item -> {
            item.setCurrency("USDT");
        });
        return resp;
    }

    private UserQuotaItemDto doQueryAccountLimit(Long userId, String subBizType) {
        APIResponse<AccountLimitQueryResponse> response = accountLimitApi.queryAccountLimit(APIRequest.instance(AccountLimitQueryRequest.builder().userId(userId).bizType("NFT").subBizType(subBizType).build()));
        baseHelper.checkResponse(response);
        if(response.getData() == null) {
            return null;
        }
        return UserQuotaItemDto.builder().userId(userId).type(subBizType).amount(new BigDecimal(response.getData().getValue())).currency(response.getData().getAsset()).build();
    }


    @PostMapping("/quota/user-quota/editAccountLimit")
    public CommonRet<Boolean> editAccountLimit(@RequestBody UserAmountLimitEditDto userQuotaEditDto) {

        Long userId = baseHelper.getUserId();
        if (null == userId) {
            return new CommonRet<>();
        }

        if(!validate(userQuotaEditDto.getSecurity2faDto(),userId)) {
            throw new BusinessException(NftAssetErrorCode.USER_QUOTA_CODE_ERROR);
        }

        if(userQuotaEditDto.getUserQuotaItemDto().getAmount().compareTo(BigDecimal.ZERO) <= 0 || userQuotaEditDto.getUserQuotaItemDto().getAmount()
        .compareTo(tradeConfig.getMaxAccountLimitAmount()) > 0) {
            throw new BusinessException(NftAssetErrorCode.USER_QUOTA_MODIFY_SAVE_ERROR);
        }

        AccountLimitEditRequest request = initAccountLimitEditRequest(userQuotaEditDto, userId);
        APIResponse<Void> response = accountLimitApi.editAccountLimit(APIRequest.instance(request));
        return new CommonRet<>(baseHelper.isOk(response));
    }

    private AccountLimitEditRequest initAccountLimitEditRequest(UserAmountLimitEditDto userQuotaEditDto, Long userId) {
        LimitDetailDto build = LimitDetailDto.builder().value(userQuotaEditDto.getUserQuotaItemDto().getAmount().toString()).build();
        if(StringUtils.equals(verifyFreeBizType,"VerifyFreePurchase")){
            build.setKey("oneTimeLimit");
        }else if(StringUtils.equals(verifyFreeBizType,"VerifyFreeBuy")){
            build.setKey("dailyLimit");
        }
        ArrayList<LimitDetailDto> objects = new ArrayList<>();
        objects.add(build);
        return AccountLimitEditRequest.builder().userId(userId).bizType("NFT").subBizType(verifyFreeBizType).operator(userId.toString()).
                asset("USDT")
                .limitDetailList(
                        objects
                ).build();
    }

    private boolean validate(Security2faDto request,Long userId) {

        SecurityDto securityDto = SecurityDto.builder()
                .userId(userId)
                .emailVerifyCode(request.getEmailVerifyCode())
                .googleVerifyCode(request.getGoogleVerifyCode())
                .mobileVerifyCode(request.getMobileVerifyCode())
                .yubikeyVerifyCode(request.getYubikeyVerifyCode())
                .build();
        APIResponse<Boolean> response = security2faApi.validate2fa(securityDto);

        if (response != null && Objects.equals(response.getStatus(), APIResponse.Status.OK)) {
            return response.getData();
        }
        return false;
    }

}
