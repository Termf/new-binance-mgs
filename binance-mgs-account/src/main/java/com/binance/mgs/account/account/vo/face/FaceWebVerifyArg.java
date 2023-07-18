package com.binance.mgs.account.account.vo.face;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;

/**
 * @author liliang1
 * @date 2018-12-13 15:11
 */
@ApiModel("WEB端人脸识别结果验证")
@Setter
@Getter
public class FaceWebVerifyArg implements Serializable {

    private static final long serialVersionUID = -7374362454091059340L;

    @ApiModelProperty("WEB端人脸识别结果签名")
    private String sign;

    @ApiModelProperty("WEB端人脸识别结果")
    private String data;
}
