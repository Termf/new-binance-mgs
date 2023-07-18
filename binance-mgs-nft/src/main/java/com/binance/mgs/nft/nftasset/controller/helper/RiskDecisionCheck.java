package com.binance.mgs.nft.nftasset.controller.helper;

import com.binance.master.error.GeneralCode;
import com.binance.master.models.APIRequest;
import com.binance.master.models.APIResponse;
import com.binance.master.utils.Assert;
import com.binance.master.utils.IPUtils;
import com.binance.master.utils.WebUtils;
import com.binance.mgs.nft.nftasset.vo.NftMintArg;
import com.binance.nft.assetservice.api.INftDraftApi;
import com.binance.nft.assetservice.api.data.request.NftMintFailedRequest;
import com.binance.nft.assetservice.api.mintmanager.IAdminMintManagerApi;
import com.binance.nft.assetservice.enums.MintFaildNotifyTplCodeEnum;
import com.binance.nft.bnbgtwservice.api.data.dto.UserComplianceCheckRet;
import com.binance.nft.bnbgtwservice.api.data.req.UserComplianceCheckReq;
import com.binance.nft.bnbgtwservice.api.iface.IUserComplianceApi;
import com.binance.nft.bnbgtwservice.common.enums.BusinessTypeEnum;
import com.binance.nft.bnbgtwservice.common.enums.ComplianceTypeEnum;
import com.binance.platform.mgs.base.helper.BaseHelper;
import com.binance.platform.mgs.base.vo.CommonRet;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicReference;
@Slf4j
@Component
@RequiredArgsConstructor
public class RiskDecisionCheck {
    private final BaseHelper baseHelper;
    private final IUserComplianceApi userComplianceApi;

    private final IAdminMintManagerApi adminMintManagerApi;

    private final INftDraftApi nftDraftApi;

    //默认开启
    @Value("${nft.asset.risk.decision.enabled:true}")
    private Boolean riskDecisionEnabled;


    public CommonRet check(NftMintArg request) {
        if(!riskDecisionEnabled){
            return null;
        }
        final Long userId = baseHelper.getUserId();
        if (null != userId) {
            String ip = IPUtils.getIpAddress(WebUtils.getHttpServletRequest());
            String devicePk = WebUtils.getHeader("fvideo-id");

            AtomicReference<String> collectionId = new AtomicReference();
            AtomicReference<String> network = new AtomicReference();
//            String businessType = null;
//            String requestURI = WebUtils.getHttpServletRequest().getRequestURI();
//            if ("/v1/private/nft/nft-mint/agree-risk-reminder".equalsIgnoreCase(requestURI)) {
            String businessType = BusinessTypeEnum.ENABLE_MINT.getDesc();
//            } else if ("/v1/private/nft/nft-asset/draft-info/mint/v2".equalsIgnoreCase(requestURI)) {
//                businessType = BusinessTypeEnum.MINT.getDesc();
//                collectionId.set(String.valueOf(request.getCollectionId()));
//                network.set(request.getNetwork());
//            }

            CommonRet result = this.complianceCheckSync(ip,devicePk,userId, ComplianceTypeEnum.RISK_DECISION_CHECK.getCode(),
                    businessType, collectionId==null?null:collectionId.get(), network==null?null:network.get());
//            if(result != null){
//                if ("/v1/private/nft/nft-asset/draft-info/mint/v2".equalsIgnoreCase(requestURI)){
//                    //增加用户校验错误次数
//                    adminMintManagerApi.updateUserToSuspend(APIRequest.instance(userId));
//                    //发送邮件给用户
//                    nftDraftApi.notifyForMintFailed(APIRequest.instance(NftMintFailedRequest.builder()
//                            .userId(userId)
//                            .emailTplCode(MintFaildNotifyTplCodeEnum.NFT_MINT_FAILED_ACCESS.getName())
//                            .errorCode(result.getCode())
//                            .errorMsg(result.getMessage())
//                            .build()));
//                }
                return result;
//            }
        }
        return null;
    }

    public CommonRet complianceCheckSync(String ip, String devicePk, Long userId, Integer type, String businessType, String collectionId,String collectionNetwork) {
        try {
            Assert.isTrue(userId != null,"userId must be not null");
            Assert.isTrue(ip != null,"ip must not be not null");
            Assert.isTrue(businessType != null,"businessType must be not null");
//            if(BusinessTypeEnum.MINT.getCode().equals(businessType)){
//                Assert.isTrue(collectionId != null,"collection id must be not null");
//                Assert.isTrue(collectionNetwork != null,"collection network must be not null");
//            }
            UserComplianceCheckReq req = UserComplianceCheckReq.builder()
                    .type(type)
                    .userId(userId)
                    .ip(ip)
                    .businessType(businessType)
                    .collectionId(collectionId)
                    .collectionNetwork(collectionNetwork)
                    .devicePk(devicePk)
                    .build();
            APIResponse<UserComplianceCheckRet> response = userComplianceApi.complianceCheck(APIRequest.instance(req));
            if (!(response != null && response.getStatus() == APIResponse.Status.OK)) {
                return null;
            }
            if (response.getData()!=null && !response.getData().getPass()) {
                log.warn("[risk decision] complianceCheck hit {} {}", userId, response.getData());
                CommonRet result = new CommonRet();
                result.setCode(GeneralCode.SYS_VALID.getCode());
                result.setMessage(GeneralCode.SYS_VALID.getMessage());
                result.setData(response.getData().getExtraInfo());
                return result;
            }
            return null;
        } catch (Exception e) {
            log.error("[risk decision] complianceCheckSync error", e);
        }
        return null;
    }
}
