package com.binance.mgs.nft.trade.convertor;

import com.binance.master.utils.CopyBeanUtils;
import com.binance.mgs.nft.market.utils.AesUtil;
import com.binance.mgs.nft.trade.response.*;
import com.binance.nft.assetservice.api.data.dto.NftAssetPropertiesInfoDto;
import org.springframework.util.CollectionUtils;
import org.springframework.util.ObjectUtils;

import java.util.ArrayList;
import java.util.List;

public class ProductDetailExtendMgsResponseConvertor {

    public static ProductDetailExtendMgsResponse convert(ProductDetailExtendResponse resp, String password) {

        NftInfoMgsVo nftInfoMgsVo = CopyBeanUtils.fastCopy(resp.getNftInfo(), NftInfoMgsVo.class);
        if(!CollectionUtils.isEmpty(resp.getNftInfo().getProperties())){
            List<NftAssetPropertiesInfoDto> properties = new ArrayList<>();
            resp.getNftInfo().getProperties().forEach(
                    s->{
                        NftAssetPropertiesInfoDto nftAssetPropertiesInfoDto = new NftAssetPropertiesInfoDto();
                        nftAssetPropertiesInfoDto.setPropertyType(s.getTraitType());
                        nftAssetPropertiesInfoDto.setPropertyName(s.getValue());
                        properties.add(nftAssetPropertiesInfoDto);
                    }
            );
            nftInfoMgsVo.setProperties(properties);
        }
        ArtistUserMgsInfo creator = null;
        if (!ObjectUtils.isEmpty(resp.getNftInfo().getCreator())){
            creator = CopyBeanUtils.fastCopy(resp.getNftInfo().getCreator(), ArtistUserMgsInfo.class);

            if (resp.getNftInfo().getCreator().getUserId() != null){
                creator.setUserId(AesUtil.encrypt(resp.getNftInfo().getCreator().getUserId().toString(), password));
            }
        }
        nftInfoMgsVo.setCreator(creator);

        ProductDetailMgsResponse mgsResponse = ProductDetailMgsResponse.builder()
                .productDetail(resp.getProductDetail())
                .productFee(resp.getProductFee())
                .nftInfo(nftInfoMgsVo)
                .isOwner(resp.getIsOwner())
                .maxAmountUserId(resp.getMaxAmountUserId())
                .timestamp(resp.getTimestamp())
                .build();

        ProductDetailExtendMgsResponse result = CopyBeanUtils.fastCopy(mgsResponse, ProductDetailExtendMgsResponse.class);
        result.setMysteryBoxProductDetailVo(resp.getMysteryBoxProductDetailVo());
        result.setApprove(resp.getApprove());
        result.setReportVo(resp.getReportVo());
        return result;
    }
}
