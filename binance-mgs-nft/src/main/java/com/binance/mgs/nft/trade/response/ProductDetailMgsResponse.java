package com.binance.mgs.nft.trade.response;

import com.binance.nft.tradeservice.vo.ProductDetailVo;
import com.binance.nft.tradeservice.vo.ProductFeeVo;
import io.swagger.annotations.ApiModel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@ApiModel
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductDetailMgsResponse implements Serializable {

    private static final long serialVersionUID = 8932386961627887487L;

    private ProductDetailVo productDetail;

    private ProductFeeVo productFee;

    private NftInfoMgsVo nftInfo;

    private Integer isOwner;

    private Long maxAmountUserId;

    private Long timestamp;
}
