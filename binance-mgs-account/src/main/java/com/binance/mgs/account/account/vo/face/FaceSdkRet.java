package com.binance.mgs.account.account.vo.face;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;

/**
 * @author liliang1
 * @date 2018-12-13 11:57
 */
@ApiModel("人脸识别SDK端验证结果")
@Setter
@Getter
public class FaceSdkRet implements Serializable {

    private static final long serialVersionUID = -5373778366575768462L;

    @ApiModelProperty("成功或失败页面显示标题头")
    private String title;

    @ApiModelProperty("成功或失败页面显示内容")
    private String content;

}
