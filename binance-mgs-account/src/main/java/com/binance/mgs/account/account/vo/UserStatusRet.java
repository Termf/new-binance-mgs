package com.binance.mgs.account.account.vo;

import java.io.Serializable;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

/**
 * 用户状态
 * Created by Kay.Zhao on 2021/3/7
 */
@ApiModel(description = "用户状态", value = "用户状态")
@Data
public class UserStatusRet implements Serializable {

    private static final long serialVersionUID = 5646853198314523675L;
    
    @ApiModelProperty(notes = "是否开启谷歌2次验证")
    private Boolean isUserGoogle;
    @ApiModelProperty(notes = "用户手机验证")
    private Boolean isUserMobile;
    @ApiModelProperty(name = "是否绑定邮箱")
    private Boolean isBindEmail;

}
