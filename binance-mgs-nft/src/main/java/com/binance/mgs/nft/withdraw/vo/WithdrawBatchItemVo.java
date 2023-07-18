package com.binance.mgs.nft.withdraw.vo;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.util.List;

@Data
public class WithdrawBatchItemVo {
    @ApiModelProperty("batch id")
    private Long batchId;
    @ApiModelProperty("batch number")
    private String batchNumber;
    @ApiModelProperty("withdraw nft icons")
    private List<String> nftIcons;
    @ApiModelProperty("订单类型(0-regular nft,1-mystery box)")
    private Integer batchType;
    @ApiModelProperty("网络类型")
    private String networkType;
    @ApiModelProperty("提现数量")
    private Integer quantity;
    @ApiModelProperty("目标地址")
    private String targetAddress;
    @ApiModelProperty("状态(0-pending,1-completed,2-partly completed,3-failed.)")
    private Integer status;
    @ApiModelProperty("创建时间")
    private Long createTime;
    /**
     * for history inbox
     *
     * false:nothing  true:unread
     */
    @ApiModelProperty("是否未读标志，true:unread false or null:read")
    private Boolean unreadFlag;
}
