package com.binance.mgs.nft.mint.controller;

import com.binance.master.error.BusinessException;
import com.binance.master.models.APIRequest;
import com.binance.master.models.APIResponse;
import com.binance.mgs.nft.market.vo.UserBannedInfo;
import com.binance.mgs.nft.nftasset.controller.helper.UserHelper;
import com.binance.nft.assetservice.api.INftDraftApi;
import com.binance.nft.assetservice.api.data.dto.AuditMintNftResult;
import com.binance.nft.assetservice.api.data.dto.FileUploadReq;
import com.binance.nft.assetservice.api.data.vo.MinterInfo;
import com.binance.nft.assetservice.api.mintmanager.IMintManagerApi;
import com.binance.nft.cex.wallet.api.deposit.frontend.IFileFrontendAPI;
import com.binance.nft.cex.wallet.api.deposit.frontend.data.req.GenerateFileUrlRequest;
import com.binance.nft.cex.wallet.api.deposit.frontend.data.res.CheckFileUploadResponse;
import com.binance.nft.cex.wallet.api.deposit.frontend.data.res.GenerateFileUrlResponse;
import com.binance.nft.mintservice.api.iface.NFTMintApi;
import com.binance.nft.mintservice.api.vo.NFTMintRequest;
import com.binance.nft.mintservice.api.vo.NFTMintUploadRequest;
import com.binance.nft.mintservice.api.vo.NFTMinteUploadResponse;
import com.binance.nft.mintservice.api.vo.NFTMintedResponse;
import com.binance.nft.reconcilication.api.NftWhiteListAdminApi;
import com.binance.platform.mgs.annotations.UserOperation;
import com.binance.platform.mgs.base.helper.BaseHelper;
import com.binance.platform.mgs.base.vo.CommonRet;
import io.swagger.annotations.Api;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.validation.Valid;
import java.util.List;
import java.util.Locale;


@Api
@Slf4j
@RestController
@RequestMapping("/v1")
@Setter
public class MintController {

    @Resource
    private NFTMintApi nftMintApi;

    @Resource
    private IFileFrontendAPI frontendAPI;

    @Resource
    private BaseHelper baseHelper;

    @Resource
    private UserHelper userHelper;

    @Resource
    private IMintManagerApi mintManagerApi;

    @Resource
    private NftWhiteListAdminApi nftWhiteListAdminApi;
    @Resource
    private INftDraftApi nftDraftApi;

    @Value("#{'${nft.mint.upload.suffix:.png,.jpeg,.gif,.jpg,.mp4,.mpeg,.avi,.wav,.mp3}'.split(',')}")
    private List<String> suffixList;

    @GetMapping("/private/nft/nft-mint/is-mint-switch-open")
    public CommonRet<Boolean> isMintSwitchOpen(){
        Long userId = baseHelper.getUserId();
        if (null == userId){
            return new CommonRet<>(false);
        }
        APIResponse<MinterInfo> minterInfoAPIResponse = mintManagerApi.checkUserMintable(APIRequest.instance(String.valueOf(userId)));
        baseHelper.checkResponse(minterInfoAPIResponse);
        return new CommonRet(minterInfoAPIResponse.getData()==null?false:(minterInfoAPIResponse.getData().isCreatable()));
    }

    @GetMapping("/private/nft/nft-mint/check-user-banned")
    public CommonRet<UserBannedInfo> checkUserBanned() {
        Long userId = baseHelper.getUserId();
        if (null == userId){
            return new CommonRet<>(null);
        }
        APIResponse<MinterInfo> minterInfoAPIResponse = mintManagerApi.checkUserMintable(APIRequest.instance(String.valueOf(userId)));
        baseHelper.checkResponse(minterInfoAPIResponse);
        MinterInfo minterInfo = minterInfoAPIResponse.getData();
        UserBannedInfo userBannedInfo = new UserBannedInfo();

        if(minterInfo == null){
            userBannedInfo.setAdminBanned(true);
            return new CommonRet(userBannedInfo);
        }else{
            String minterStatus = minterInfo.getStatus();
            if("Suspended".equalsIgnoreCase(minterStatus)){
                userBannedInfo.setAdminBanned(false);
                userBannedInfo.setReopenTime(minterInfo.getSuspendTime().getTime()-System.currentTimeMillis());
            }else if("Admin Banned".equalsIgnoreCase(minterStatus)) {
                userBannedInfo.setAdminBanned(true);
            }else if("Available".equalsIgnoreCase(minterStatus)) {
                return new CommonRet<>(null);
            }
            return new CommonRet(userBannedInfo);
        }
    }




    /**  获取文件上传presignUrl
     * @return
     */
    @PostMapping("/private/nft/nft-mint/gen-upload-url")
    public CommonRet<NFTMinteUploadResponse> genUploadUrl(@Valid @RequestBody NFTMintUploadRequest request) {

        final Long userId = baseHelper.getUserId();

        request.setUserId(userId.toString());
        if (!userHelper.checkUserWhiteList(userId).isMintFlag()){
            throw new BusinessException("Sorry, you haven't got the access to upload yet.");
        }

        return getNftMinteUploadResponseCommonRet(request);
    }

    /**  获取文件上传presignUrl
     * @return
     */
    @PostMapping("/private/nft/nft-mint/generic-upload-url")
    public CommonRet<NFTMinteUploadResponse> genericUploadUrl(@Valid @RequestBody NFTMintUploadRequest request) {
        return getNftMinteUploadResponseCommonRet(request);
    }

    /**
     * 获取文件上传presignUrl
     * @return
     */
    @UserOperation(eventName = "NFT_Generic_Upload", name = "NFT_Generic_Upload",
            responseKeys = {"$.code", "$.message", "$.errorMessage", "$.errorCode"},
            responseKeyDisplayNames = {"code", "message", "errorMessage", "errorCode"})
    @PostMapping("/private/nft/nft-asset/generic-upload-url")
    public CommonRet<GenerateFileUrlResponse> genericFileCenterUploadUrl(@Valid @RequestBody GenerateFileUrlRequest request) {
        if (suffixList.stream().noneMatch(x -> StringUtils.equalsIgnoreCase(x, request.getSuffix()))){
            throw new BusinessException("Sorry, file type is not supported.");
        }
        return new CommonRet<>(frontendAPI.generateFileUrl(APIRequest.instance(request)).getData());
    }

    @GetMapping("/private/nft/nft-asset/check-if-upload-success")
    public CommonRet<CheckFileUploadResponse> checkIfUploadSuccess(@RequestParam("objectKey") String objectKey,
                                                                   @RequestParam(value = "mediaType", required = false) String mediaType) {
        CheckFileUploadResponse checkFileUploadResponse = frontendAPI.checkIfFileUploadSuccess(objectKey).getData();
        if(checkFileUploadResponse.getSuccess()){
            //目的是为了通过mint那边的校验
            NFTMintUploadRequest uploadReq = new NFTMintUploadRequest();
            uploadReq.setUserId(String.valueOf(baseHelper.getUserId()));
            //此参数无意义
            uploadReq.setSuffix("check");
            uploadReq.setUrl(checkFileUploadResponse.getActualUrl());
            APIResponse<Boolean> uploadRes = nftMintApi.stampUserUrl(APIRequest.instance(uploadReq));
            if (null == uploadRes || null == uploadRes.getData() || !uploadRes.getData()){
                return new CommonRet<>(CheckFileUploadResponse.builder().success(false).build());
            }
            //发起mint nft image/video的异步检查
            checkAudit(checkFileUploadResponse.getActualUrl(), mediaType);
        }
        return new CommonRet<>(checkFileUploadResponse);
    }

    private void checkAudit(String nftUrl, String mediaType)  {

        APIResponse<AuditMintNftResult> response = nftDraftApi.checkMintNftV2(APIRequest.instance(
                FileUploadReq.builder().fileUrl(nftUrl).type(mediaType).userId(baseHelper.getUserId()).build()));
        baseHelper.checkResponse(response);
    }

    private CommonRet<NFTMinteUploadResponse> getNftMinteUploadResponseCommonRet(@RequestBody @Valid NFTMintUploadRequest request) {
        if (suffixList.stream().noneMatch(x -> StringUtils.equalsIgnoreCase(x, request.getSuffix()))){
            throw new BusinessException("Sorry, file type is not supported.");
        }
        request.setSuffix(request.getSuffix().toLowerCase(Locale.ROOT));
        APIResponse<NFTMinteUploadResponse> response = nftMintApi.genUploadUrl(APIRequest.instance(request));
        baseHelper.checkResponse(response);
        return new CommonRet<>(response.getData());
    }

    /**  mint
     * @return
     */
    @PostMapping("/private/nft/nft-mint/open-mystery-box")
    public CommonRet<NFTMintedResponse> openMysteryBox(@Valid @RequestBody NFTMintRequest request) {
        APIResponse<NFTMintedResponse> response = nftMintApi.openMysteryBox(APIRequest.instance(request));
        baseHelper.checkResponse(response);
        return new CommonRet<>(response.getData());
    }
}
