package com.binance.mgs.account.account.vo.face;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;

/**
 * @author liliang1
 * @date 2018-12-13 14:36
 */
@ApiModel("人脸识别初始化结果V2")
@Setter
@Getter
public class FaceInitRetV2 implements Serializable {

    private static final long serialVersionUID = -2967976724544792852L;

    /** SDK 端初始化生产的二维码 */
    @ApiModelProperty("SDK端的初始化二维码值")
    private String qrCode;

    /** PC 端初始化生产的人脸识别页面 */
    @ApiModelProperty("WEB端的人脸识别地址")
    private String livenessUrl;
    
    /**二维码有效周期（秒）*/
    @ApiModelProperty("二维码有效周期(秒)")
    private long qrCodeValidSeconds;
    
    @ApiModelProperty("fase私有云offer返回参数")
    private String offerAnswer;

    /** PC 端初始化生产的人脸识别页面 */
    @ApiModelProperty("私有云token返回地址")
    private String faceUrl;
    
}
