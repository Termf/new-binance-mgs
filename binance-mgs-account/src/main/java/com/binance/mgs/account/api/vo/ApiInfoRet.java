package com.binance.mgs.account.api.vo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * Created by Fei.Huang on 2018/8/13.
 */
@Data
@ApiModel("API信息")
public class ApiInfoRet implements Serializable {
    /**
     * 
     */
    private static final long serialVersionUID = -1613335259298657026L;

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
    private String type;

    @ApiModelProperty
    private boolean apiEmailVerify;

    @ApiModelProperty
    private Date createEmailSendTime;

    @ApiModelProperty("标记报税专用字段")
    private boolean taxReport;

    @ApiModelProperty
    private Long apiManageIpConfigId;

    @ApiModelProperty
    private String apiManageIpConfigName;
}
