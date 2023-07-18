package com.binance.mgs.account.authcenter.vo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;

@Data
@ApiModel("登录返回")
public class LoginRet implements Serializable {
    /**
    * 
    */
    private static final long serialVersionUID = 4390211032213045581L;
    private String csrfToken;
    // resultMap.put("emailVerified", true);
    private boolean emailVerified;
    // 返回值
    private boolean gauth;
    private boolean mobileSecurity;
    private boolean securityKey;
    private boolean legacySecurityKey;

    private String token;
    private boolean confirmTips;
    private boolean deviceChangeConfirm;
    private String currentDeviceId;
    private boolean disable;
    private String authStatus;
    private boolean reLogin;
    private boolean pendingReview;
    @ApiModelProperty("用户手机号")
    private String mobile;
    @ApiModelProperty("用户手机号code")
    private String mobileCode;
    @ApiModelProperty("用户id，为了兼容pnk老系统故返回userId，等pnkweb下线后删除")
    private String userId;
    @ApiModelProperty("用户登陆邮箱")
    private String email;
    @ApiModelProperty("代表用户是否只是绑定了邮箱或者手机其中一个2fa")
    private boolean onlyBindMobileOrEmail;
    @ApiModelProperty("代表用户是否新设备")
    private boolean newDeviceFLag;
    // oauth code
    private String code;
    @ApiModelProperty(readOnly = true, notes = "是否非合规国家且有资产")
    private boolean needComplianceAndHashAsset = false;

    @ApiModelProperty(readOnly = true, notes = "非合规国家国家code")
    private String needComplianceCountry;

    @ApiModelProperty(readOnly = true, notes = "非合规国家剩余时间")
    private String needComplianceTimeLeft;


    private String refreshToken;

    private String bncLocation;
}
