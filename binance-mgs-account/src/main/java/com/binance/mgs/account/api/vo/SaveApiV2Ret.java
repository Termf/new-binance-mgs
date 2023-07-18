package com.binance.mgs.account.api.vo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

@Data
@ApiModel("API保存返回信息")
public class SaveApiV2Ret implements Serializable {
    private static final long serialVersionUID = -7714782777942944190L;

    @ApiModelProperty
    private String id;

    @ApiModelProperty
    private String userId;

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
    private boolean apiEmailVerify;

    @ApiModelProperty
    private Date createEmailSendTime;

    @ApiModelProperty("是否存在margin账号，仅子账号api管理在使用")
    private Boolean isExistMarginAccount;

    @ApiModelProperty("是否存在future账号，仅子账号api管理在使用")
    private Boolean isExistFutureAccount;

    @ApiModelProperty("flexLine信用额度子账号")
    private Boolean isFlexLineCreditUser = false;

    @ApiModelProperty("flexLine交易子账号")
    private Boolean isFlexLineTradingUser = false;

    @ApiModelProperty("标记报税专用字段")
    private boolean taxReport;

    @ApiModelProperty("API-KEY类型")
    private String type;
}

