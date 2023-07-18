package com.binance.mgs.account.account.vo.subuser;

import com.binance.platform.openfeign.jackson.BigDecimal2String;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;

/**
 * @author dana.d
 */
@ApiModel("子账户充值列表response")
@Data
public class QuerySubUserDepositListRet {

    @ApiModelProperty("id")
    private Long id;

    @ApiModelProperty("转账数量")
    @BigDecimal2String
    private BigDecimal transferAmount;

    @ApiModelProperty("时间")
    private Date insertTime;

    @ApiModelProperty("状态")
    private Integer status;

    @ApiModelProperty("浏览器url")
    private String addressUrl;

    @ApiModelProperty("用户id")
    private Long userId;

    @ApiModelProperty("币种")
    private String coin;

    @ApiModelProperty("地址")
    private String address;

    @ApiModelProperty("tag")
    private String addressTag;

    @ApiModelProperty("币种别名")
    private String assetLabel;

    @ApiModelProperty("txid")
    private String txId;

    @ApiModelProperty("当前确认次数")
    private Long curConfirmTimes;

    @ApiModelProperty("确认次数")
    private Long confirmTimes;

    @ApiModelProperty("解锁次数")
    private Long unlockConfirm;

    @ApiModelProperty("浏览器")
    private String txUrl;

    @ApiModelProperty("状态名")
    private String statusName;

    @ApiModelProperty("转账内型")
    private Integer transferType;

    @ApiModelProperty("审核备注")
    private String comments;

    @ApiModelProperty("网络")
    private String network;

    @ApiModelProperty("账户类型")
    private Integer walletType;

    @ApiModelProperty("充值找回状态")
    private Integer selfReturnStatus;
}
