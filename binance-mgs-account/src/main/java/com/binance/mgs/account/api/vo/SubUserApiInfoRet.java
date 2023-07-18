package com.binance.mgs.account.api.vo;

import java.util.Date;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

/**
 * Created by pcx
 */
@Data
@ApiModel("API信息")
public class SubUserApiInfoRet {

    @ApiModelProperty
    private String email;

    @ApiModelProperty
    private String id;

    @ApiModelProperty
    private String userId;

    @ApiModelProperty
    private Boolean isExistMarginAccount;

    @ApiModelProperty
    private Boolean isExistFutureAccount;

    @ApiModelProperty("是否是统一账户")
    private Boolean isPortfolioMarginRetailUser;

    @ApiModelProperty("flexLine信用额度子账号")
    private Boolean isFlexLineCreditUser = false;

    @ApiModelProperty("flexLine交易子账号")
    private Boolean isFlexLineTradingUser = false;

    @ApiModelProperty
    private Integer keyId;

    @ApiModelProperty
    private String apiKey;

    @ApiModelProperty
    private String apiName;

    @ApiModelProperty
    private String secretKey;

    @ApiModelProperty
    private String tradeIp;

    @ApiModelProperty
    private String withdrawIp;

    @ApiModelProperty
    private String ruleId;

    @ApiModelProperty
    private int status;

    @ApiModelProperty
    private boolean disableStatus;

    @ApiModelProperty
    private String info;

    @ApiModelProperty
    private Date createTime;

    @ApiModelProperty
    private Date updateTime;

    @ApiModelProperty
    private boolean enableWithdrawStatus;

    @ApiModelProperty
    private String withdrawVerifycode;

    @ApiModelProperty
    private Date withdrawVerifycodeTime;

    @ApiModelProperty
    private boolean withdraw;

    @ApiModelProperty
    private String uuid;

    @ApiModelProperty
    private String type;

    @ApiModelProperty
    private boolean apiEmailVerify;

    @ApiModelProperty
    private Date createEmailSendTime;

    @ApiModelProperty
    private Long apiManageIpConfigId;

    @ApiModelProperty
    private String apiManageIpConfigName;

    @ApiModelProperty
    private boolean canResetEnableTradeTime;
}
