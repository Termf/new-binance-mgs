package com.binance.mgs.account.authcenter.vo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;

@Data
@ApiModel("三方注册返回")
public class RegisterByThirdRet implements Serializable {
    /**
     *
     */
    private static final long serialVersionUID = 4302211032213045581L;
    private String csrfToken;
    // oauth code
    private String code;

    private String token;
    private String currentDeviceId;
    private boolean disable;
    private String userId;
    @ApiModelProperty("用户登陆邮箱")
    private String email;

    @ApiModelProperty(readOnly = true, notes = "newDisableLogicFLag是否是新的用户禁用流程")
    private boolean newDisableLogicFLag=false;

    @ApiModelProperty(readOnly = true, notes = "是否非合规国家且有资产")
    private boolean needComplianceAndHashAsset = false;
    @ApiModelProperty(readOnly = true, notes = "非合规国家国家code")
    private String needComplianceCountry;

    @ApiModelProperty(readOnly = true, notes = "非合规国家剩余时间")
    private String needComplianceTimeLeft;

    private String refreshToken;
}