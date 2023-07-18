package com.binance.mgs.nft.deposit;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@ApiModel
@Data
@Builder
public class FetchChainInfoRequest {
    @ApiModelProperty(required = true)
    private String contractAddress;
    @ApiModelProperty(required = true)
    private final String networkType;
    @ApiModelProperty(required = true)
    private List<String> tokenIds;
}
