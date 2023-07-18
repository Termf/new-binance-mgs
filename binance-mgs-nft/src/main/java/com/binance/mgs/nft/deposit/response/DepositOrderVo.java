package com.binance.mgs.nft.deposit.response;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.util.Date;

@Data
public class DepositOrderVo {
    @ApiModelProperty("id")
    private Long id;
    @ApiModelProperty("order id")
    private String orderId;
    @ApiModelProperty("user id")
    private Long userId;
    @ApiModelProperty("网络类型")
    private String networkType;
    @ApiModelProperty("协议")
    private String nftProtocol;
    @ApiModelProperty("合约地址")
    private String contractAddress;
    @ApiModelProperty("tokenId")
    private String nftTokenId;
    @ApiModelProperty("钱包地址")
    private String walletAddress;
    @ApiModelProperty("状态")
    private Integer status;
    @ApiModelProperty("错误信息")
    private String errorMsg;
    @ApiModelProperty("确认次数")
    private Integer ackTimes;
    @ApiModelProperty("nft类型")
    private Byte nftType;
    @ApiModelProperty("nft名称")
    private String nftName;
    @ApiModelProperty("nft的url")
    private String nftUrl;
    @ApiModelProperty("交易hash")
    private String transactionHash;
    @ApiModelProperty("创建时间")
    private Date createTime;
    @ApiModelProperty("更新时间")
    private Date updateTime;
    @ApiModelProperty("备注")
    private String orderNote;

    /**
     * for history inbox
     *
     * false:nothing  true:unread
     */
    @ApiModelProperty("是否未读标志，true:unread false or null:read")
    private Boolean unreadFlag;
}
