package com.binance.mgs.account.account.vo.face;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;

import javax.validation.constraints.NotNull;
import java.io.Serializable;

/**
 * @author liliang1
 * @date 2018-12-13 14:13
 */
@ApiModel("人脸识别初始化参数V2")
@Setter
@Getter
public class FaceInitArgV2 implements Serializable {

    private static final long serialVersionUID = 7259844968470300269L;

    @ApiModelProperty("业务编号(对应人脸识别邮件中的id的值)")
    @NotNull
    private String transId;

    @ApiModelProperty("业务类型(对应人脸识别邮件中的type的值)")
    @NotNull
    private FaceTransType type;
    
    @ApiModelProperty("face私有云offer接口透传参数)")
    @NotNull
    private String offerSdf;
    
    @ApiModelProperty("face私有云offer接口透传参数)")
    @NotNull
    private String offerType;
    
    @ApiModelProperty("face私有云offer接口透传参数)")
    @NotNull
    private String offerVideoTransform;
}
