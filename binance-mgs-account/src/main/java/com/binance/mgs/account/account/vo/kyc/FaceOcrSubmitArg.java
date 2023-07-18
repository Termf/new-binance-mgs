package com.binance.mgs.account.account.vo.kyc;

import java.io.Serializable;

import io.swagger.annotations.ApiModelProperty;
import io.swagger.annotations.ApiOperation;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class FaceOcrSubmitArg implements Serializable {
    private static final long serialVersionUID = -4702493475032374046L;

    private Integer kycType;
    private String type;
    private String face;
    private String front;
    private String back;
    private String flowDefine;

    @ApiModelProperty("用户上传文件接口返回的文件唯一号, 如果当前接口中传入了则会忽略face的二进制文件数据")
    private String faceFileKey;
    @ApiModelProperty("用户上传文件接口返回的文件唯一号, 如果当前接口中传入了则会忽略front的二进制文件数据")
    private String frontFileKey;
    @ApiModelProperty("用户上传文件接口返回的文件唯一号, 如果当前接口中传入了则会忽略back的二进制文件数据")
    private String backFileKey;
}
