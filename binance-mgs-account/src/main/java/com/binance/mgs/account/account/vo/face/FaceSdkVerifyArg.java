package com.binance.mgs.account.account.vo.face;

import com.binance.platform.mgs.base.vo.CommonArg;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;

import javax.validation.constraints.NotNull;

/**
 * @author liliang1
 * @date 2018-10-10 18:01
 */
@Getter
@Setter
@ApiModel("SDK方式进行人脸识别请求参数")
public class FaceSdkVerifyArg extends CommonArg {

    private static final long serialVersionUID = -4141050909423684096L;

    @ApiModelProperty("业务标识")
    @NotNull
    private String transId;

    /**
     * SDK 验证检验码
     */
    @ApiModelProperty("SDK验证检验码")
    @NotNull
    private String delta;

    @ApiModelProperty("最佳照片Base64串")
    @NotNull
    private String imageBest;

    @ApiModelProperty("背景照片Base64串")
    @NotNull
    private String imageEnv;

    @ApiModelProperty("动作1照片Base64串")
    private String imageAction1;

    @ApiModelProperty("动作2照片Base64串")
    private String imageAction2;

    @ApiModelProperty("动作3照片Base64串")
    private String imageAction3;
}
