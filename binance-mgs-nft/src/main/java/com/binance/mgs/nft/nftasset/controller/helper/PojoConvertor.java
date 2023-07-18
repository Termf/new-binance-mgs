package com.binance.mgs.nft.nftasset.controller.helper;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.binance.master.models.APIResponse;
import com.binance.mgs.nft.market.utils.AesUtil;
import com.binance.mgs.nft.nftasset.vo.*;
import com.binance.nft.assetservice.api.data.request.function.CollectionNftRenamingRequest;
import com.binance.nft.assetservice.api.data.vo.*;
import com.binance.nft.assetservice.api.data.request.NftTransactionRequest;
import com.binance.nft.assetservice.api.data.vo.MysteryBoxSimpleVoTotal;
import com.binance.nft.assetservice.api.data.vo.NftEventVo;
import com.binance.nft.assetservice.api.data.vo.function.NftLayerConfigVo;
import com.binance.nft.bnbgtwservice.api.data.dto.UserSimpleAccountDto;
import org.mapstruct.IterableMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.TimeZone;

@Mapper(componentModel = "spring", imports = {LocalDateTime.class, TimeZone.class})
@Component
public abstract class PojoConvertor {

    @Value("${nft.version.provenance.switch:false}")
    private boolean showAssetSwitch;
    @Value("${nft.aes.password}")
    private String AES_PASSWORD;

    public abstract MysteryBoxSimpleTotalVo copyMysteryBoxTotalPage(MysteryBoxSimpleVoTotal mysteryBoxSimpleVoTotal);
    public abstract NftTransactionRequest copyNftTransactionArg(NftTransactionArg arg);

    public abstract MysteryBoxSimpleSerialsTotalVo copyMysteryBoxSerialsTotalPage(MysteryBoxSerialsSimpleVoTotal data);

    public abstract List<MysteryBoxSimpleItemVo> copyMysteryBoxItemTotalPage(List<MysteryBoxItemSimpleVo> data);

    @Mapping(source = "records", target = "records", qualifiedByName = "copyNftEventList")
    public abstract Page<NftEventVoRet>copyNftEventVoRetPage(Page<NftEventVo> eventVoPage);

    @Named("copyNftEventList")
    @IterableMapping(qualifiedByName = "copyNftEventVoRet")
    public abstract List<NftEventVoRet>copyNftEventList(List<NftEventVo> eventVoPage);

    @Named("copyNftEventVoRet")
    @Mapping(target = "asset", qualifiedByName = "assetCopy")
    @Mapping(target = "amount", qualifiedByName = "amountCopy")
    @Mapping(target = "userId", source = "userId", qualifiedByName = "encryptUserId")
    public abstract NftEventVoRet copyNftEventVoRet(NftEventVo nftEventVo);

    @Named("encryptUserId")
    protected String encryptUserId(Long userId){
        if (Objects.isNull(userId)){
            return "";
        }
        return AesUtil.encrypt(userId.toString(), AES_PASSWORD);
    }

    @Named("assetCopy")
    protected String assetCopy(String asset){
        return showAssetSwitch ? asset : null;
    }
    @Named("amountCopy")
    protected BigDecimal amountCopy(BigDecimal amount){
        return showAssetSwitch ? amount : null;
    }


    public abstract UserSimpleAccountVo copyAccountSimple(UserSimpleAccountDto userSimpleAccountDto);

    public abstract Page<NftTransactionLpdVo> copyTransactionLpd(Page<NftTransactionVo> data);

    public abstract CollectionCreateArg copyCollectionCreateVo2Arg(NftLayerConfigVo data);

    public abstract NftLayerConfigVo copyCollectionCreateArg2Vo(CollectionCreateArg arg);
    @Mapping(source = "collectionName", target = "toCollectionName")
    public abstract CollectionNftRenamingRequest copyNftRenamingArg2Request(ShadowCollectionConfigArg arg);

    public abstract CollectionNamingVo copyCollectionNamingVo2Mgs(com.binance.nft.assetservice.api.data.vo.function.CollectionNamingVo data);

    public abstract List<CollectionNftDto> copyCollectionNft2MgsList(List<com.binance.nft.assetservice.api.data.vo.function.CollectionNftDto> data);

    @Mapping(source = "nftInfoIdList", target = "nftInfoId")
    @Mapping(source = "collectionId", target = "toCollectionId")
    public abstract CollectionNftRenamingRequest copyNftTransformArg2Request(ShadowCollectionNftTransformArg arg);
}
