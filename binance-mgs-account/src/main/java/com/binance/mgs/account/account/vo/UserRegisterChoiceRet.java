package com.binance.mgs.account.account.vo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

@Data
@ApiModel("用户注册时相关选项")
public class UserRegisterChoiceRet {

    @ApiModelProperty(value = "是否EU用户")
    private Boolean isEU = false;

    @ApiModelProperty(value = "是否绑定手机")
    private Boolean isBindMobile = false;

    @ApiModelProperty(value = "是否勾选邮箱推广")
    private Boolean isEmailPromote = false;

    @ApiModelProperty(value = "用户选择的居住国家")
    private String residentCountry;

    @ApiModelProperty(value = "用户选择国家所属区域,eg:EU")
    private String region;
}