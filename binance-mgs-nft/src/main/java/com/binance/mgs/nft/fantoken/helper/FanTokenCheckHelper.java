package com.binance.mgs.nft.fantoken.helper;

import com.alibaba.fastjson.JSON;
import com.binance.master.constant.Constant;
import com.binance.master.error.BusinessException;
import com.binance.master.models.APIRequest;
import com.binance.master.models.APIResponse;
import com.binance.master.utils.IPUtils;
import com.binance.master.utils.WebUtils;
import com.binance.nft.bnbgtwservice.api.data.dto.AccountComplianceRet;
import com.binance.nft.bnbgtwservice.api.data.dto.FanTokenComplianceAssetDto;
import com.binance.nft.fantoken.vo.SimpleTeamInfo;
import com.binance.platform.mgs.base.helper.BaseHelper;
import com.binance.nft.bnbgtwservice.api.data.dto.UserComplianceCheckRet;
import com.binance.nft.bnbgtwservice.api.data.req.UserComplianceCheckReq;
import com.binance.nft.bnbgtwservice.api.iface.IUserComplianceApi;
import com.binance.nft.bnbgtwservice.common.enums.ComplianceTypeEnum;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * <h1>FanToken 相关的一些校验工具</h1>
 * */
@SuppressWarnings("all")
@Slf4j
@Service
@RequiredArgsConstructor
public class FanTokenCheckHelper {

    private final BaseHelper baseHelper;
    private final IUserComplianceApi userComplianceApi;

    /** gcc 合规使用到的常量定义 */
    private final static String HEADER_BNC_LOCATION = "BNC-Location";
    private final static Set<String> HEADER_BNC_LOCATION_VALUE = Stream.of("AE", "BINANCE_BAHRAIN_BSC", "KZ").collect(Collectors.toSet());

    /**
     * <h2>强制 KYC 校验</h2>
     * */
    public void userComplianceValidate(Long userId) {

        UserComplianceCheckReq req = UserComplianceCheckReq.builder()
                .type(ComplianceTypeEnum.KYC_CHECK.getCode())
                .userId(userId)
                .ip(IPUtils.getIp())
                .front(false)
                .build();

        APIResponse<UserComplianceCheckRet> response = userComplianceApi.complianceCheck(APIRequest.instance(req));
        baseHelper.checkResponse(response);
        log.info("current user KYC response: [{}], [{}]", userId, JSON.toJSONString(response.getData()));
        if (!response.getData().getPass()) {
            log.warn("current user KYC check fail {} {}", userId, response.getData());
            throw new BusinessException(response.getData().getErrorCode(), response.getData().getErrorMessage());
        }
    }

    /**
     * <h2>用户是否有 kyc</h2>
     * */
    public boolean hasKyc(Long userId) {

        UserComplianceCheckReq req = UserComplianceCheckReq.builder()
                .type(ComplianceTypeEnum.KYC_CHECK.getCode())
                .userId(userId)
                .ip(WebUtils.getRequestIp())
                .front(false)
                .build();

        APIResponse<UserComplianceCheckRet> response = userComplianceApi.complianceCheck(APIRequest.instance(req));
        baseHelper.checkResponse(response);
        log.info("current user KYC response: [{}], [{}]", userId, JSON.toJSONString(response.getData()));

        return response.getData().getPass();
    }

    /**
     * <h2>是否处于灰度上线状态</h2>
     * */
    public boolean isGray() {
        String env = WebUtils.getHeader(Constant.GRAY_ENV_HEADER);
        return StringUtils.isNotBlank(env) && !"normal".equals(env);
    }

    /**
     * <h2>gcc 合规, 获取请求中的 BncLocation</h2>
     * 需要注意:
     *  1. 如果是登录用户, 通过 account-compliance 去查询
     *  2. 如果不是登录用户, 获取 header 中的 BNC-Location
     * */
    public String getGccComplianceBncLocation(Long userId) {

        if (null == userId) {
            String bncLocation = WebUtils.getHeader(HEADER_BNC_LOCATION);
            return StringUtils.isNotBlank(bncLocation) && HEADER_BNC_LOCATION_VALUE.contains(bncLocation)
                    ? bncLocation : StringUtils.EMPTY;
        }

        try {
            UserComplianceCheckReq req = UserComplianceCheckReq.builder()
                    .userId(userId)
                    .ip(IPUtils.getIp())
                    .build();
            APIResponse<AccountComplianceRet> apiResponse = userComplianceApi.gccCompliance(APIRequest.instance(req));
            baseHelper.checkResponse(apiResponse);
            if (null != apiResponse && null != apiResponse.getData() && !apiResponse.getData().getPass()) {
                String bncLocation = apiResponse.getData().getBncLocation();
                return StringUtils.isNotBlank(bncLocation) && HEADER_BNC_LOCATION_VALUE.contains(bncLocation)
                        ? bncLocation : StringUtils.EMPTY;
            }
        } catch (Exception ex) {
            log.error("getGccComplianceBncLocation has some error: [{}], [{}]", userId, ex.getMessage(), ex);
        }

        return StringUtils.EMPTY;
    }

    /**
     * <h2>当前请求是否触发了巴林合规</h2>
     * */
    public boolean isTriggerBahreynCompliance(Long userId) {
        return StringUtils.isNotBlank(getGccComplianceBncLocation(userId));
    }

    /**
     * <h2>gcc 合规要求下, 对当前用户开放的合规币种</h2>
     * */
    public FanTokenComplianceAssetDto fanTokenComplianceAsset(Long userId) {

        try {
            if (null == userId) {
                String bncLocation = getGccComplianceBncLocation(userId);
                if (StringUtils.isNotBlank(bncLocation)) {
                    APIResponse<FanTokenComplianceAssetDto> apiResponse =
                            userComplianceApi.fanTokenComplianceAssetFromBncLocation(APIRequest.instance(bncLocation));
                    baseHelper.checkResponse(apiResponse);
                    return apiResponse.getData();
                }
            } else {
                UserComplianceCheckReq req = UserComplianceCheckReq.builder()
                        .userId(userId)
                        .ip(IPUtils.getIp())
                        .build();
                APIResponse<FanTokenComplianceAssetDto> apiResponse = userComplianceApi.fanTokenComplianceAssetFromUser(
                        APIRequest.instance(req));
                baseHelper.checkResponse(apiResponse);
                return apiResponse.getData();
            }
        } catch (Exception ex) {
            log.error("fanTokenComplianceAsset has some error: [{}], [{}]", userId, ex.getMessage(), ex);
        }

        return FanTokenComplianceAssetDto.builder()
                .pass(Boolean.TRUE)
                .assets(Collections.emptyList())
                .build();
    }

    /**
     * <h2>过滤出符合 gcc 合规的 team 信息</h2>
     * */
    public List<SimpleTeamInfo> getGccComplianceTeamInfo(boolean isGray, Long userId, List<SimpleTeamInfo> infos) {

        // 灰度环境或球队信息为空, 直接返回传递进来的数据
        if (isGray || CollectionUtils.isEmpty(infos)) {
            return infos;
        }

        FanTokenComplianceAssetDto complianceAsset = fanTokenComplianceAsset(userId);
        if (null != complianceAsset && !complianceAsset.getPass()) {
            // 合规的资产
            List<String> assets = complianceAsset.getAssets();
            if (CollectionUtils.isEmpty(assets)) {
                return Collections.emptyList();
            }
            return infos.stream().filter(i -> StringUtils.isBlank(i.getTeamToken()) || assets.contains(i.getTeamToken()))
                    .collect(Collectors.toList());
        }

        return infos;
    }

    /**
     * <h2>世界杯接口中判断是否是 SG 用户</h2>
     * */
    public boolean isSGUserForWorldCup(Long userId) {

        UserComplianceCheckReq req = UserComplianceCheckReq.builder()
                .userId(userId)
                .ip(WebUtils.getRequestIp())
                .front(false)
                .build();

        try {
            APIResponse<UserComplianceCheckRet> response = userComplianceApi.fantokenWorldCupSGComplianceCheck(APIRequest.instance(req));
            baseHelper.checkResponse(response);
            // 如果命中了 sg 用户, pass 会返回 false; 所以, 这里的返回需要取反
            return !response.getData().getPass();
        } catch (Exception ex) {
            log.error("check sg user for worldcup has some error: [userId={}], [ex={}]", userId, ex.getMessage(), ex);
        }

        return false;
    }

    /**
     * <h2>MemberShip Participate 相关的合规性校验</h2>
     * true: 通过合规; false: 不通过合规
     * */
    public boolean membershipParticipateComplianceCheck(Long userId) {

        UserComplianceCheckReq req = UserComplianceCheckReq.builder()
                .userId(userId)
                .ip(WebUtils.getRequestIp())
                .front(false)
                .build();

        try {
            APIResponse<UserComplianceCheckRet> response = userComplianceApi.fantokenMemberShipParticipateComplianceCheck(APIRequest.instance(req));
            baseHelper.checkResponse(response);
            return response.getData().getPass();
        } catch (Exception ex) {
            log.error("check user for membership participate has some error: [userId={}], [ex={}]", userId, ex.getMessage(), ex);
        }

        return true;
    }

    /**
     * <h2>MemberShip Award 相关的合规性校验</h2>
     * true: 通过合规; false: 不通过合规
     * */
    public boolean membershipComplianceCheck(Long userId) {

        UserComplianceCheckReq req = UserComplianceCheckReq.builder()
                .userId(userId)
                .ip(WebUtils.getRequestIp())
                .front(false)
                .build();

        try {
            APIResponse<UserComplianceCheckRet> response = userComplianceApi.fantokenMemberShipAwardComplianceCheck(APIRequest.instance(req));
            baseHelper.checkResponse(response);
            return response.getData().getPass();
        } catch (Exception ex) {
            log.error("check user for membership award has some error: [userId={}], [ex={}]", userId, ex.getMessage(), ex);
        }

        return true;
    }

    /**
     * <h2> SG 合规校验 </h2>
     * */
    public void userSGComplianceValidate(Long userId) {

        UserComplianceCheckReq req = UserComplianceCheckReq.builder()
                .userId(userId)
                .ip(WebUtils.getRequestIp())
                .front(false)
                .build();

        APIResponse<UserComplianceCheckRet> response = userComplianceApi.fantokenWorldCupSGComplianceCheck(APIRequest.instance(req));
        baseHelper.checkResponse(response);
        if (!response.getData().getPass()) {
            log.warn("current user sg compliance check fail {} {}", userId, response.getData());
            throw new BusinessException(response.getData().getErrorCode(), response.getData().getErrorMessage());
        }
    }
}
