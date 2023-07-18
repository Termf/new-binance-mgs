package com.binance.mgs.account.account.vo.face;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;

import javax.validation.constraints.NotNull;
import java.io.Serializable;

/**
 * @author liliang1
 * @date 2018-12-13 13:57
 */
@ApiModel("人脸识别二维码验证")
@Setter
@Getter
public class FaceSdkQrCodeArg implements Serializable {

    private static final long serialVersionUID = 4794799880337171397L;

    @ApiModelProperty("二维码的业务标识")
    @NotNull
    private String transId;
}
