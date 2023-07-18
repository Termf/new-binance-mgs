package com.binance.mgs.nft.nftasset.vo;

import com.binance.mgs.nft.nftasset.response.RiskAuditResponse;
import com.binance.platform.openfeign.jackson.Long2String;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;

@Data
public class NftMintRet extends RiskAuditResponse implements Serializable {
    private String status;
    @Long2String
    private Long nftDraftId;
    @Long2String
    private Long nftInfoId;
    @ApiModelProperty("nft草稿mint进度")
    private BigDecimal mintProcess;
    @ApiModelProperty("nft草稿的封面图片")
    private String coverUrl;
    @ApiModelProperty("nft草稿的名称")
    private String nftTitle;
    @ApiModelProperty("nft草稿的描述")
    private String description;
    @ApiModelProperty("nft草稿的tokenId")
    private String tokenId;
    @ApiModelProperty("nft草稿的合约地址")
    private String contractAddress;
    @ApiModelProperty("nft草稿的库存量")
    @Long2String
    private Long quantity;
    private String mediaType;
}
