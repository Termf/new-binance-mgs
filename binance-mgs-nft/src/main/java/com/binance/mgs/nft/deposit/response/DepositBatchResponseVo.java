package com.binance.mgs.nft.deposit.response;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.util.List;

@Data
public class DepositBatchResponseVo {

    @ApiModelProperty("id")
    private Long id;
    @ApiModelProperty("batch id")
    private String batchId;
    @ApiModelProperty("网络类型")
    private String networkType;
    @ApiModelProperty("图片列表")
    private List<String> imageList;
    @ApiModelProperty("交易hash")
    private String transactionHash;
    @ApiModelProperty("状态(0-processing,1-finished,2-part-completed,-1-fail)")
    private Integer status;
    @ApiModelProperty("创建时间")
    private Long createTime;

    private Boolean unreadFlag;
}
