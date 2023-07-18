package com.binance.mgs.account.account.vo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;

/**
 * 用户转态
 *
 * @author wang-bijie
 */
@ApiModel(description = "用户状态", value = "用户状态")
@Data
public class UserStatusExRet implements Serializable {


    /**
     * 
     */
    private static final long serialVersionUID = -5145139788944390222L;

    @ApiModelProperty(notes = "是否开启谷歌2次验证")
    private Boolean isUserGoogle;
    @ApiModelProperty(notes = "用户手机验证")
    private Boolean isUserMobile;
    @ApiModelProperty(notes = "BNB手续费开关是否开启")
    private Boolean isUserFee;
    @ApiModelProperty(notes = "app交易是否禁用")
    private Boolean isUserTradeApp;
    @ApiModelProperty(notes = "是否开启提币白名单")
    private Boolean isWithdrawWhite;


    // @ApiModelProperty(notes = "用户是否激活")
    // private Boolean isUserActive;
    //
    // @ApiModelProperty(notes = "用户是否禁用")
    // private Boolean isUserDisabled;
    //
    // @ApiModelProperty(notes = "用户锁定")
    // private Boolean isUserLock;
    //
    // @ApiModelProperty(notes = "特殊用户")
    // private Boolean isUserSpecial;
    //
    // @ApiModelProperty(notes = "种子用户")
    // private Boolean isUserSend;
    //
    // @ApiModelProperty(notes = "协议确定")
    // private Boolean isUserProtocol;
    //
    // @ApiModelProperty(notes = "用户强制修改密码")
    // private Boolean isUserForcedPassword;
    //
    // @ApiModelProperty(notes = "子账号")
    // private Boolean isUserSubAccount;
    //
    // @ApiModelProperty(notes = "申购是否禁用")
    // private Boolean isUserPurchase;
    //
    // @ApiModelProperty(notes = "交易是否禁用")
    // private Boolean isUserTrade;
    //
    // @ApiModelProperty(notes = "微信绑定")
    // private Boolean isUserWeixin;
    //
    // @ApiModelProperty(notes = "api交易是否禁用")
    // private Boolean isUserTradeApi;
    //
    // @ApiModelProperty(notes = "是否删除用户")
    // private Boolean isUserDelete;
}
