package com.binance.mgs.nft.nftasset.controller;

import com.alibaba.fastjson.JSON;
import com.binance.master.error.BusinessException;
import com.binance.master.error.GeneralCode;
import com.binance.master.models.APIRequest;
import com.binance.master.models.APIResponse;
import com.binance.master.utils.CopyBeanUtils;
import com.binance.master.utils.IPUtils;
import com.binance.master.utils.StringUtils;
import com.binance.master.utils.WebUtils;
import com.binance.mgs.nft.core.redis.RedisCommonConfig;
import com.binance.mgs.nft.mysterybox.helper.MysteryBoxI18nHelper;
import com.binance.mgs.nft.nftasset.controller.helper.*;
import com.binance.mgs.nft.nftasset.response.ActivityBurnCheckDto;
import com.binance.mgs.nft.nftasset.response.NftAssetDetailResponse;
import com.binance.mgs.nft.nftasset.response.PreMintCheckResponse;
import com.binance.mgs.nft.nftasset.response.RiskAuditResponse;
import com.binance.mgs.nft.nftasset.vo.*;
import com.binance.mgs.nft.reconciliation.helper.ReconciliationHelper;
import com.binance.nft.activityservice.response.LotteryConfigResponse;
import com.binance.nft.assetservice.api.INftAssetApi;
import com.binance.nft.assetservice.api.INftAssetLogApi;
import com.binance.nft.assetservice.api.INftDraftApi;
import com.binance.nft.assetservice.api.INftInfoApi;
import com.binance.nft.assetservice.api.data.dto.AuditMintNftResult;
import com.binance.nft.assetservice.api.data.dto.FileUploadReq;
import com.binance.nft.assetservice.api.data.request.*;
import com.binance.nft.assetservice.api.data.response.MintNftResp;
import com.binance.nft.assetservice.api.data.response.NftMintProcessResponse;
import com.binance.nft.assetservice.api.data.vo.*;
import com.binance.nft.assetservice.api.data.vo.audit.ImageFileAuditResp;
import com.binance.nft.assetservice.api.data.vo.detail.NftDetailDto;
import com.binance.nft.assetservice.api.function.ILayerInfoApi;
import com.binance.nft.assetservice.api.mintmanager.IMintManagerApi;
import com.binance.nft.assetservice.enums.MintFaildNotifyTplCodeEnum;
import com.binance.nft.assetservice.util.S3Utils;
import com.binance.nft.bnbgtwservice.api.data.dto.UserComplianceCheckRet;
import com.binance.nft.bnbgtwservice.api.data.req.UserComplianceCheckReq;
import com.binance.nft.bnbgtwservice.api.iface.IUserComplianceApi;
import com.binance.nft.bnbgtwservice.common.enums.ComplianceTypeEnum;
import com.binance.nft.mystery.api.iface.NFTMysteryBoxAdminApi;
import com.binance.nft.mystery.api.iface.NFTMysteryBoxApi;
import com.binance.nft.mystery.api.vo.*;
import com.binance.nft.reconciliaction.constant.FeeTypeEnum;
import com.binance.platform.mgs.annotations.UserOperation;
import com.binance.platform.mgs.base.helper.BaseHelper;
import com.binance.platform.mgs.base.vo.CommonPageRet;
import com.binance.platform.mgs.base.vo.CommonRet;
import com.binance.platform.mgs.config.CaffeineCacheConfig;
import io.swagger.annotations.Api;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang.math.NumberUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.TimeUnit;

import static com.binance.nft.assetservice.constant.NftAssetErrorCode.NFT_ASSET_NOT_FOUND;
import static com.binance.nft.assetservice.constant.NftAssetErrorCode.USER_NOT_IN_WHITE_LIST;

@Api
@Slf4j
@RequestMapping("/v1/")
@RestController
@RequiredArgsConstructor
public class NftAssetController {

    private final INftDraftApi nftDraftApi;
    private final BaseHelper baseHelper;
    private final UserHelper userHelper;
    private final INftAssetApi nftAssetApi;

    private final INftInfoApi nftInfoApi;
    private final NFTMysteryBoxApi mysteryBoxApi;

    private final NftAssetHelper nftAssetHelper;

    private final ActivityCR7Helper activityCR7Helper;

    private final IUserComplianceApi userComplianceApi;

    private final MysteryBoxI18nHelper mysteryBoxI18nHelper;

    private final ReconciliationHelper reconciliationHelper;

    private final ActivityHelper activityHelper;

    private final MysteryBoxHelper mysteryBoxHelper;

    private final NFTMysteryBoxAdminApi nftMysteryBoxAdminApi;

    private final IMintManagerApi mintManagerApi;

    @Autowired
    private RedisTemplate<String,Object> redisTemplate;

    private final ILayerInfoApi iLayerInfoApi;

    private final RiskDecisionCheck riskDecisionCheck;

    private final FreezeReasonHelper freezeReasonHelper;

    private final INftAssetLogApi nftAssetLogApi;

    @Value("${nft.mint.activity.fee:0}")
    private BigDecimal ACTIVITY_FEE;

    @UserOperation(eventName = "NFT_Mint_Check", name = "NFT_Mint_Check",
            responseKeys = {"$.code", "$.message", "$.errorMessage", "$.errorCode"},
            responseKeyDisplayNames = {"code", "message", "errorMessage", "errorCode"})
    @PostMapping("private/nft/nft-mint/pre-mint-check")
    public CommonRet<PreMintCheckResponse> preMintCheck(@RequestBody(required=false) MintPreCheckRequest request) throws Exception{
        Long userId = baseHelper.getUserId();
        if (null == userId){
            return new CommonRet<>();
        }

        return nftAssetHelper.preMintCheck(userId, request);
    }


    @PostMapping("private/nft/nft-asset/mint/url/exist")
    public CommonRet<Boolean> checkMintUrlExist(@RequestBody @Valid @NotNull MintUrlExistCheckRequest request){
        return new CommonRet<>(
                Objects.equals(
                        S3Utils.fetchHeaderResponseCode(request.getUrl()), HttpStatus.OK.value()
                )
        );
    }


    @UserOperation(eventName = "NFT_Burn_Nft", name = "NFT_Burn_Nft",
            responseKeys = {"$.code", "$.message", "$.errorMessage", "$.errorCode"},
            responseKeyDisplayNames = {"code", "message", "errorMessage", "errorCode"})
    @PostMapping("private/nft/nft-asset/burn")
    public CommonRet<BigDecimal> burnNFT(@RequestBody @Valid @NotNull NftBurnArg nftBurnArg) throws Exception {
        Long userId = baseHelper.getUserId();
        if (null == userId){
            return new CommonRet<>();
        }
        userComplianceValidate(userId);

        NftDetailDto nftDetailDto = nftAssetHelper.getNFTInfoDetailByNft(userId,nftBurnArg.getNftId());

        if(Objects.isNull(nftDetailDto)){
            throw new BusinessException(NFT_ASSET_NOT_FOUND.getCode(), NFT_ASSET_NOT_FOUND.getMessage());
        }
        Pair<List<Long>,List<Long>> responseAPIResponse = nftAssetHelper.checkForbiddenForBurnByDetailInfo(
                userId.toString(),
                NftInfoCheckBurnReq.builder()
                        .isOwner(nftDetailDto.getOwner().getUserId().compareTo(userId)==0?1:0)
                        .creatorId(nftDetailDto.getCreator().getUserId().toString())
                        .isOnChain(nftDetailDto.isOnChain())
                        .nftInfoId(nftDetailDto.getId())
                        .marketStatus(nftDetailDto.getMarketStatus())
                        .build()
        );
        if(Objects.nonNull(responseAPIResponse) && Objects.nonNull(responseAPIResponse.getLeft()) &&
                !responseAPIResponse.getLeft().contains(nftBurnArg.getNftId())){
            throw new BusinessException(USER_NOT_IN_WHITE_LIST.getCode(), USER_NOT_IN_WHITE_LIST.getMessage());
        }
        ReconciliationHelper.ReconciliationDto reconciliation = reconciliationHelper.getReconciliationDtoByFeeType(
                "BSC".equalsIgnoreCase(nftDetailDto.getNetwork())? FeeTypeEnum.WITHDRAW_BSC.getCode():FeeTypeEnum.WITHDRAW_ERC.getCode());

        APIResponse<Void> response = nftInfoApi.burnNft(APIRequest.instance(UserBurnNftRequest.builder()
                .userId(userId)
                .collectionId(nftDetailDto.getSerialsNo())
                .asset(reconciliation.getCurrency())
                .burnFee(reconciliation.getAmount())
                .nftInfoItems(
                        Arrays.asList(
                                UserBurnNftRequest.NftInfoItem.builder()
                                        .nftType(nftDetailDto.getNftType())
                                        .tokenId(nftDetailDto.getTokenId())
                                        .nftId(nftDetailDto.getId())
                                        .build()))
                .build()));
        baseHelper.checkResponse(response);
        return new CommonRet<>();
    }

    @RequestMapping("/private/nft/nft-mint/agree-risk-reminder")
    @UserOperation(
            eventName = "ENABLE_MINT",
            name = "启用铸造",
            sendToBigData = true,
            sendToDb = true,
            requestKeys = {"businessType"},
            requestKeyDisplayNames = {"businessType"},
            responseKeys = {"success","data.decisionCode"},
            responseKeyDisplayNames = {"isNotHit","extend.decision_code"}
    )
    public CommonRet<RiskAuditResponse> agreeRiskReminder(@RequestParam(value="businessType",defaultValue = "ENABLE_MINT",required = false) String businessType){
        Long userId = baseHelper.getUserId();
        if (null == userId){
            return new CommonRet<>();
        }
        CommonRet checkResult = riskDecisionCheck.check(null);
        if(Objects.nonNull(checkResult)){
            return checkResult;
        }
        log.error("this is not an error,but important,{} just agree the risk reminder", baseHelper.getUserId());
        String key = String.format(RedisCommonConfig.NFT_MINT_AGREED_RISK_REMINDER, baseHelper.getUserId());
        redisTemplate.opsForValue().set(key,System.currentTimeMillis(), 1000, TimeUnit.DAYS);
        return new CommonRet<>();
    }

    @RequestMapping("/private/nft/nft-mint/hasAgreedRiskReminder")
    public CommonRet<Boolean> hasAgreedRiskReminder(){
        Long userId = baseHelper.getUserId();
        if (null == userId){
            return new CommonRet<>(false);
        }
        String key = String.format(RedisCommonConfig.NFT_MINT_AGREED_RISK_REMINDER, baseHelper.getUserId());
        Object agreedRiskRemind = redisTemplate.opsForValue().get(key);
        if(agreedRiskRemind!=null){
            return new CommonRet<>(true);
        }
        return new CommonRet<>(false);
    }

    @PostMapping("private/nft/nft-asset/draft-info/mint")
    @UserOperation(eventName = "NFT_Mint", name = "NFT_Mint", sendToBigData = true, sendToDb = true,
            responseKeys = {"$.code","$.message","$.data", "$.errorMessage", "$.errorCode"},
            responseKeyDisplayNames = {"code","message","data","errorMessage","errorCode"})
    public CommonRet<NftMintRet> mintNft(@RequestBody @Valid @NotNull NftMintArg request) throws Exception {

        NftMintRet nftMintRet = new NftMintRet();
        if (request.getQuantity() > NumberUtils.INTEGER_ONE) {
            nftMintRet.setStatus("F");
            return new CommonRet<>(nftMintRet);
        }

        final Long userId = baseHelper.getUserId();

        if (!userHelper.checkUserWhiteList(userId).isMintFlag()){
            nftMintRet.setStatus("WHITE-BLOCK");
            return new CommonRet<>(nftMintRet);
        }

        userComplianceValidate(userId);

        ReconciliationHelper.ReconciliationDto reconciliation = reconciliationHelper.getReconciliationDto();

        Integer mintCount = userHelper.checkMintCount(userId);

        NftMintRequest nftMintRequest = CopyBeanUtils.fastCopy(request, NftMintRequest.class);
        nftMintRequest.setUserId(userId);
        nftMintRequest.setAsset(reconciliation.getCurrency());
        nftMintRequest.setMintFee(reconciliation.getAmount().subtract(ACTIVITY_FEE));
        APIResponse<Long> apiResponse = nftDraftApi.mintNft(APIRequest.instance(nftMintRequest));

        if(!baseHelper.isOk(apiResponse) && mintCount > 0) {
            userHelper.decrMintCount(userId,mintCount);
        }

        baseHelper.checkResponse(apiResponse);

        nftMintRet.setNftDraftId(apiResponse.getData());
        if (APIResponse.OK.getStatus().equals(apiResponse.getStatus())) {
            nftMintRet.setStatus("S");
        } else {
            nftMintRet.setStatus("F");
        }

        return new CommonRet<>(nftMintRet);
    }

    @PostMapping("private/nft/nft-asset/draft-info/mint/v2")
    @UserOperation(eventName = "NFT_Mint", name = "NFT_Mint", sendToBigData = true, sendToDb = true,
            requestKeys = {"businessType","network","collectionId"},
            requestKeyDisplayNames = {"businessType","collectionNetwork","collectionId"},
            responseKeys = {"$.code","$.message","$.data", "$.errorMessage", "$.errorCode"},
            responseKeyDisplayNames = {"code","message","data","errorMessage","errorCode"})
    public CommonRet<NftMintRetV2> mintNftV2(@RequestBody @Valid @NotNull NftMintArg request) throws Exception {
        NftMintRetV2 nftMintRet = new NftMintRetV2();
        if (request.getQuantity() > NumberUtils.INTEGER_ONE) {
            nftMintRet.setStatus("F");
            return new CommonRet<>(nftMintRet);
        }

        final Long userId = baseHelper.getUserId();
        if (null == userId){
            return new CommonRet<>();
        }
        CommonRet<PreMintCheckResponse> preMintCheckResponseCommonRet = nftAssetHelper.preMintCheck(userId, null);
        if(Objects.nonNull(preMintCheckResponseCommonRet.getData()) && Objects.nonNull(preMintCheckResponseCommonRet.getData().getTplCode())){
            if(preMintCheckResponseCommonRet.getData().getTplCode().equalsIgnoreCase(PreMintCheckResponse.TplCodeEnum.MINT_COUNT_LIMIT.getCode())){
                nftDraftApi.notifyForMintFailed(APIRequest.instance(NftMintFailedRequest.builder()
                        .userId(userId)
                        .emailTplCode(MintFaildNotifyTplCodeEnum.NFT_MINT_FAILED_LIMIT.getName())
                        .errorCode(preMintCheckResponseCommonRet.getData().getTplCode())
                        .build()));
            }else{
                nftDraftApi.notifyForMintFailed(APIRequest.instance(NftMintFailedRequest.builder()
                        .userId(userId)
                        .emailTplCode(MintFaildNotifyTplCodeEnum.NFT_MINT_FAILED_ACCESS.getName())
                        .errorCode(preMintCheckResponseCommonRet.getData().getTplCode())
                        .build()));
            }
            nftMintRet.setStatus("F");
            return new CommonRet<>(nftMintRet);
        }
        userComplianceValidate(userId);

        String ip = IPUtils.getIpAddress(WebUtils.getHttpServletRequest());
        String devicePk = WebUtils.getHeader("fvideo-id");

        NftMintRequest nftMintRequest = CopyBeanUtils.fastCopy(request, NftMintRequest.class);
        nftMintRequest.setUserId(userId);
        nftMintRequest.setIp(ip);
        nftMintRequest.setDevicePk(devicePk);
        APIResponse<MintNftResp> response = nftDraftApi.mintNftV3(APIRequest.instance(nftMintRequest));
        baseHelper.checkResponse(response);

        MintNftResp mintNftResp = Optional.ofNullable(response)
                .map(item -> item.getData())
                .orElse(MintNftResp.fail(Collections.emptyMap()));
        if(!mintNftResp.isSuccess()){
            CommonRet result = new CommonRet();
            result.setCode(GeneralCode.SYS_VALID.getCode());
            Map<String, Object> params = mintNftResp.getParams();
            freezeReasonHelper.enrichMessage(params);
            result.setData(params);
            return result;
        }else{
            nftMintRet.setNftDraftId(mintNftResp.getDraftId());
            nftMintRet.setStatus("S");
            return new CommonRet<>(nftMintRet);
        }
    }

    private void userComplianceValidate(Long userId) {
        UserComplianceCheckReq req = UserComplianceCheckReq.builder()
                .type(ComplianceTypeEnum.genType(ComplianceTypeEnum.KYC_CHECK,ComplianceTypeEnum.CLEAR_CHECK))
                .userId(userId)
                .front(false)
                .build();
        APIResponse<UserComplianceCheckRet> response = userComplianceApi.complianceCheck(APIRequest.instance(req));
        baseHelper.checkResponse(response);
        if(!response.getData().getPass()) {
            log.warn("kycCheck check fail {} {}", userId, response.getData());
            throw new BusinessException(response.getData().getErrorCode(), response.getData().getErrorMessage());
        }
    }

    @GetMapping("private/nft/nft-asset/mint/process/{nft-draft-id}")
    public CommonRet<NftMintRet> mintProcess(@PathVariable("nft-draft-id") Long nftDraftId) {

        Long userId = baseHelper.getUserId();
        if (null == userId) {
            return new CommonRet<>();
        }
        APIResponse<NftMintProcessResponse> apiResponse = nftDraftApi.getNftDraftProcess(nftDraftId);
        baseHelper.checkResponse(apiResponse);
        NftMintProcessResponse data = apiResponse.getData();
        NftMintRet nftMintRet = new NftMintRet();
        nftMintRet.setNftDraftId(nftDraftId);
        nftMintRet.setNftInfoId(data.getNftInfoId());
        nftMintRet.setMintProcess(data.getProcess());
        nftMintRet.setNftTitle(data.getNftTitle());
        nftMintRet.setDescription(data.getDescription());
        nftMintRet.setCoverUrl(data.getCoverUrl());
        nftMintRet.setQuantity(data.getQuantity());
        nftMintRet.setContractAddress(data.getContractAddress());
        nftMintRet.setTokenId(data.getTokenId());
        nftMintRet.setMediaType(data.getMediaType());

        if (APIResponse.OK.getStatus().equals(apiResponse.getStatus())) {
            if (BigDecimal.ONE.compareTo(nftMintRet.getMintProcess()) == 0) {
                nftMintRet.setStatus("S");
            } else {
                nftMintRet.setStatus("P");
            }
        } else {
            nftMintRet.setStatus("F");
        }

        return new CommonRet<>(nftMintRet);
    }

    @GetMapping("private/nft/nft-asset/mint/process/v2/{nft-draft-id}")
    public CommonRet<NftMintRetV2> mintProcessV2(@PathVariable("nft-draft-id") String nftDraftId) {

        Long userId = baseHelper.getUserId();
        if (null == userId) {
            return new CommonRet<>();
        }
        APIResponse<NftMintProcessResponse> apiResponse = nftDraftApi.getNftDraftProcessV2(nftDraftId);
        baseHelper.checkResponse(apiResponse);
        NftMintProcessResponse data = apiResponse.getData();
        NftMintRetV2 nftMintRet = new NftMintRetV2();
        nftMintRet.setNftDraftId(nftDraftId);
        nftMintRet.setNftInfoId(data.getNftInfoId());
        nftMintRet.setMintProcess(data.getProcess());
        nftMintRet.setNftTitle(data.getNftTitle());
        nftMintRet.setDescription(data.getDescription());
        nftMintRet.setCoverUrl(data.getCoverUrl());
        nftMintRet.setQuantity(data.getQuantity());
        nftMintRet.setContractAddress(data.getContractAddress());
        nftMintRet.setTokenId(data.getTokenId());
        nftMintRet.setMediaType(data.getMediaType());

        if (APIResponse.OK.getStatus().equals(apiResponse.getStatus())) {
            if (BigDecimal.ONE.compareTo(nftMintRet.getMintProcess()) == 0) {
                nftMintRet.setStatus("S");
            } else {
                nftMintRet.setStatus("P");
            }
        } else {
            nftMintRet.setStatus("F");
        }

        return new CommonRet<>(nftMintRet);
    }

    @GetMapping("private/nft/nft-asset/single-asset/{nft-info-id}")
    public CommonRet<NftProductVo> getNftAsset(@PathVariable("nft-info-id") Long nftInfoId) throws Exception {

        Long userId = baseHelper.getUserId();
        if (null == userId) {
            return new CommonRet<>();
        }
        List<NftProductVo> data = nftAssetHelper.getUserNftAssetByInfoId(userId, nftInfoId);
        if (CollectionUtils.isEmpty(data)) {
            return new CommonRet<>();
        }
        return new CommonRet<>(data.get(0));
    }

//    @Cacheable(value = CaffeineCacheConfig.DEFAULT_CACHE_MIDDLE)
    @GetMapping("friendly/nft/nft-asset/single-asset/{nft-info-id}")
    public CommonRet<NftProductResponse> getSingleNftAsset(@PathVariable("nft-info-id") Long nftInfoId) throws Exception {

        Long userId = baseHelper.getUserId();
        List<NftProductVo> data = nftAssetHelper.getUserNftAssetByInfoId(userId, nftInfoId);
        if (CollectionUtils.isEmpty(data)) {
            return new CommonRet<>();
        }
        //用本地缓存补齐properties信息的统计信息
        NftProductVo nftProductVo = data.get(0);
        if(Objects.nonNull(nftProductVo)) {
            //此处不用fastcopy，因为fastcopy不会转换类型
            NftProductResponse result = CopyBeanUtils.copy(nftProductVo, NftProductResponse.class);
            nftAssetHelper.fillPropertiesByAddress(nftProductVo.getSerialsNo(), result.getProperties());

            APIResponse<NftLayerVo> nftLayerVoAPIResponse = iLayerInfoApi.findLayerDetailById(APIRequest.instance(nftProductVo.getSerialsNo()));
            baseHelper.checkResponse(nftLayerVoAPIResponse);

            result.setSerialsName(nftLayerVoAPIResponse.getData().getLayerName());
            result.setSerialsAvatarUrl(nftLayerVoAPIResponse.getData().getAvatarUrl());
            return new CommonRet<>(result);
        }else{
            return new CommonRet<>();
        }
    }

    @GetMapping("friendly/nft/nft-asset/properties-list")
    public CommonRet<Map<String, List>> propertiesList(@RequestParam("collectionId") Long collectionId) throws Exception {
        if(!Objects.nonNull(collectionId)){
            return new CommonRet<>();
        }
        Map<String, List> data = nftAssetHelper.getPropertiesByCollectionId(collectionId);
        if (MapUtils.isEmpty(data)) {
            return new CommonRet<>();
        }
        return new CommonRet<>(data);
    }

    @GetMapping("private/nft/nft-asset/single-asset/un-opened-box/{serials-no}")
    public CommonRet<UnopenedBoxAssetDetail> unOpenedBoxNftAsset(@PathVariable("serials-no") Long serialsNo) throws Exception {

        Long userId = baseHelper.getUserId();
        if (null == userId) {
            return new CommonRet<>();
        }

        UnopenedBoxAssetDetail unopenedBoxASSETDETAIL = UnopenedBoxAssetDetail.builder().build();

        APIResponse<MysteryBoxSerialVo> boxSerialVoAPIResponse = nftAssetApi.queryMysteryBoxSerial(
                APIRequest.instance(MysteryBoxSerialRequest.builder()
                        .userId(baseHelper.getUserId())
                        .serialsNo(serialsNo)
                        .build()));
        baseHelper.checkResponse(boxSerialVoAPIResponse);
        MysteryBoxSerialVo mysteryBoxSerialVo = boxSerialVoAPIResponse.getData();

        unopenedBoxASSETDETAIL.setMysteryBoxSerialVo(mysteryBoxSerialVo);
        MysteryBoxProductDetailVo mysteryBoxProductDetailVo = mysteryBoxHelper.queryMysteryBoxDetailForMeta(serialsNo);
//        APIResponse<MysteryBoxProductDetailVo> productDetailVoAPIResponse = mysteryBoxApi.queryMysteryBoxDetailForMeta(APIRequest.instance(
//                QueryMysteryBoxDetailForMetaRequest.builder()
//                        .serialNo(serialsNo)
//                        .build()
//        ));
//        baseHelper.checkResponse(productDetailVoAPIResponse);
//        MysteryBoxProductDetailVo mysteryBoxProductDetailVo = productDetailVoAPIResponse.getData();
//        checkAndSetSecondDelay(mysteryBoxProductDetailVo);
        mysteryBoxI18nHelper.doI18n(mysteryBoxProductDetailVo);
        unopenedBoxASSETDETAIL.setMysteryBoxProductDetailVo(mysteryBoxProductDetailVo);
        return new CommonRet<>(unopenedBoxASSETDETAIL);
    }

    private void checkAndSetSecondDelay(MysteryBoxProductDetailVo mysteryBoxDetailForMeta){
        if (Objects.isNull(mysteryBoxDetailForMeta)
                || Objects.isNull(mysteryBoxDetailForMeta.getSecondMarketSellingDelay())
                || Objects.isNull(mysteryBoxDetailForMeta.getStartTime())){

            CommonPageRequest<ListMysteryBoxRequest> boxRequest = CommonPageRequest.<ListMysteryBoxRequest>builder()
                    .params(ListMysteryBoxRequest.builder().batchId(String.valueOf(mysteryBoxDetailForMeta.getSerialsNo())).build())
                    .page(1).size(1).build();
            APIResponse<CommonPageResponse<ListMysteryBoxResponse>> response = nftMysteryBoxAdminApi.listMysteryBox(APIRequest.instance(boxRequest));
            baseHelper.checkResponse(response);
            if (CollectionUtils.isEmpty(response.getData().getData()) || response.getData().getData().get(0).getListStartTime() == null) {
                return;
            }
            Date listStartTime = response.getData().getData().get(0).getListStartTime();
            final long c = System.currentTimeMillis();
            if (listStartTime.getTime() >= c) {
                Long sellingDelayRemaining = listStartTime.getTime() - c;
                Long hourRemaining = sellingDelayRemaining / 3600000;
                mysteryBoxDetailForMeta.setDuration(String.valueOf(sellingDelayRemaining));
                mysteryBoxDetailForMeta.setSecondMarketSellingDelay(hourRemaining);
            }
        } else {
            final Long secondMarketSellingDelay = mysteryBoxDetailForMeta.getSecondMarketSellingDelay();
            final Long openTime = mysteryBoxDetailForMeta.getStartTime().getTime() + secondMarketSellingDelay * 60 * 60 * 1000;
            final long c = System.currentTimeMillis();
            if (openTime.compareTo(c) >= 0){
                mysteryBoxDetailForMeta.setDuration(String.valueOf(openTime - c));
            } else {
                mysteryBoxDetailForMeta.setSecondMarketSellingDelay(null);
            }
        }
//        if(mysteryBoxProductDetailVo == null || mysteryBoxProductDetailVo.getSecondMarketSellingDelay() == null || mysteryBoxProductDetailVo.getStartTime() == null) {
//            return;
//        }
//        final Long openTime = mysteryBoxProductDetailVo.getStartTime().getTime() + mysteryBoxProductDetailVo.getSecondMarketSellingDelay() * 60 * 60 * 1000;
//        final long c = System.currentTimeMillis();
//        if (openTime.compareTo(c) < 0){
//            mysteryBoxProductDetailVo.setSecondMarketSellingDelay(null);
//        }
    }

    @GetMapping("private/nft/nft-asset/single-asset/opened-box/{item-id}")
    public CommonRet<OpenedBoxAssetDetail> openedBoxNftAsset(@PathVariable("item-id") Long itemId) throws Exception {

        Long userId = baseHelper.getUserId();
        if (null == userId) {
            return new CommonRet<>();
        }

        OpenedBoxAssetDetail openedBoxAssetDetail = OpenedBoxAssetDetail.builder().build();

        APIResponse<MysteryBoxItemVo> mysteryBoxItemVoAPIResponse = nftAssetApi.queryMysteryBoxItem(
                APIRequest.instance(MysteryBoxItemRequest.builder()
                        .userId(baseHelper.getUserId())
                        .itemId(itemId)
                        .build()));
        baseHelper.checkResponse(mysteryBoxItemVoAPIResponse);
        MysteryBoxItemVo mysteryBoxItemVo = mysteryBoxItemVoAPIResponse.getData();

        openedBoxAssetDetail.setMysteryBoxItemVo(mysteryBoxItemVo);
        APIResponse<MysteryBoxProductDetailVo> productDetailVoAPIResponse = mysteryBoxApi.queryMysteryBoxDetailForMeta(APIRequest.instance(
                QueryMysteryBoxDetailForMetaRequest.builder()
                        .itemId(itemId)
                        .build()
        ));
        baseHelper.checkResponse(productDetailVoAPIResponse);

        MysteryBoxProductDetailVo mysteryBoxProductDetailVo = productDetailVoAPIResponse.getData();
        checkAndSetSecondDelay(mysteryBoxProductDetailVo);
        mysteryBoxI18nHelper.doI18n(mysteryBoxProductDetailVo);

        openedBoxAssetDetail.setMysteryBoxProductDetailVo(mysteryBoxProductDetailVo);
        return new CommonRet<>(openedBoxAssetDetail);
    }

    @GetMapping("private/nft/nft-asset/wallet-asset")
    public CommonRet<List<NftSimpleInfoVo>> getWalletNftAssetList(){

        Long userId = baseHelper.getUserId();
        if (Objects.isNull(userId)){
            return new CommonRet<>();
        }

        return new CommonRet<>(nftAssetHelper.getWalletUserNftAsset(userId));
    }

    @GetMapping("private/nft/nft-asset/avatar-asset")
    public CommonPageRet<NftSimpleInfoVo> getAvatarNftAssetList(@RequestParam("page") int page,
                                                                @RequestParam("size") int size){

        Long userId = baseHelper.getUserId();
        if (Objects.isNull(userId)){
            return new CommonPageRet<>();
        }

        return nftAssetHelper.getAvatarUserNftAsset(userId, page, size);
    }


    /**
     * 资产详情
     * @return
     */
    @GetMapping("friendly/nft/nft-asset/asset-detail")
    public CommonRet<NftAssetDetailResponse> nftAssetDetail(Long productId, Long nftInfoId) throws Exception {
        Long userId = baseHelper.getUserId();
        if(Objects.isNull(productId) &&Objects.isNull(nftInfoId)) {
            throw new BusinessException(GeneralCode.ILLEGAL_PARAM);
        }
        NftAssetDetailResponse response = null;
        if(Objects.isNull(productId)) {
            response = nftAssetHelper.queryNftAssetDetailByNftInfoId(userId, nftInfoId);
        } else {
            response = nftAssetHelper.queryNftAssetDetailByProductId(userId, productId);
        }
        activityCR7Helper.pendingCR7Info(response);
        nftAssetHelper.appendDexFlag(response);
        return new CommonRet<>(response);
    }

    @GetMapping("public/nft/nft-asset/mint/config")
    public CommonRet<HashMap> mintConfig(){
        return nftAssetHelper.getMintConfig();
    }


    @GetMapping("private/nft/nft-asset/burn/claim-check")
    public CommonRet<ActivityBurnCheckDto> nftBurnInfoCheck(){

        Long userId = baseHelper.getUserId();
        if (Objects.isNull(userId)){
            return new CommonRet<>();
        }

        final LotteryConfigResponse activityInfo = activityHelper.getActivityInfo();
        if (Objects.isNull(activityInfo)
                || System.currentTimeMillis() - activityInfo.getStartTime().getTime() < 0){
            return new CommonRet<>();
        }
        final long remainTime = activityInfo.getEndTime().getTime() - System.currentTimeMillis();
        if (remainTime <= 0){
            return new CommonRet<>();
        }
        if (activityInfo.getLeftTickets() > 0){
            return new CommonRet<>(
                    ActivityBurnCheckDto.builder()
                            .collectionId(activityInfo.getCollectionId())
                            .collectionName(activityInfo.getCollectionName())
                            .remainTime(Objects.isNull(activityInfo.getEndTime()) ? 0 : remainTime)
                            .build()
            );
        }

        final APIResponse<Boolean> apiResponse = nftAssetApi.checkUserCollection(userId, activityInfo.getCollectionId());
        baseHelper.checkResponse(apiResponse);

        if (apiResponse.getData()){
            return new CommonRet<>(
                    ActivityBurnCheckDto.builder()
                            .collectionId(activityInfo.getCollectionId())
                            .collectionName(activityInfo.getCollectionName())
                            .remainTime(Objects.isNull(activityInfo.getEndTime()) ? 0 : remainTime)
                            .build()
            );
        }

        return new CommonRet<>();
    }

    @GetMapping("public/nft/nft-asset-event/price-history/{nft-info-id}")
    @Cacheable(value = CaffeineCacheConfig.DEFAULT_CACHE_MIDDLE)
    public CommonRet<List<NftPriceHistoryVo>> getNftPriceHistory(@PathVariable("nft-info-id") Long nftInfoId) throws Exception {
        if (Objects.isNull(nftInfoId)) {
            throw new BusinessException(GeneralCode.ILLEGAL_PARAM);
        }
        APIResponse<List<NftPriceHistoryVo>> response = nftAssetLogApi.fetchNftPriceHistory(nftInfoId);
        baseHelper.checkResponse(response);
        return new CommonRet<>(response.getData());
    }

    /**
     * <h2>NFT Url 送审 Hive</h2>
     * */
    @PostMapping("/private/nft/nft-asset/draft-info/check-mint-nft")
    public CommonRet<AuditMintNftResult> checkMintNft(@RequestBody @NotNull FileUploadReq request) {

        Long userId = baseHelper.getUserId();
        if (null == userId) {
            return new CommonRet<>();
        }

        request.setUserId(userId);
        if (StringUtils.isBlank(request.getFileUrl()) || StringUtils.isBlank(request.getType())
                || request.getType().split("/").length != 2) {
            log.error("check mint nft url, but param illegal: [req={}]", JSON.toJSONString(request));
            throw new BusinessException(GeneralCode.ILLEGAL_PARAM);
        }

        APIResponse<AuditMintNftResult> response = nftDraftApi.checkMintNftInternal(APIRequest.instance(request));
        baseHelper.checkResponse(response);
        return new CommonRet<>(response.getData());
    }

    @PostMapping("/private/nft/nft-asset/draft-info/round-check-mint-nft")
    public CommonRet<ImageFileAuditResp> roundCheckMintNft(@RequestBody @NotNull FileUploadReq request) {

        Long userId = baseHelper.getUserId();
        if (null == userId) {
            return new CommonRet<>();
        }

        request.setUserId(userId);
        if (StringUtils.isBlank(request.getFileUrl())) {
            log.error("round check mint nft url, but param illegal: [req={}]", JSON.toJSONString(request));
            throw new BusinessException(GeneralCode.ILLEGAL_PARAM);
        }

        APIResponse<ImageFileAuditResp> response = nftDraftApi.roundCheckMintNft(APIRequest.instance(request));
        baseHelper.checkResponse(response);
        return new CommonRet<>(response.getData());
    }
}
