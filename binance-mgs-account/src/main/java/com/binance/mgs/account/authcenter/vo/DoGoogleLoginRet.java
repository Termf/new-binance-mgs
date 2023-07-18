package com.binance.mgs.account.authcenter.vo;

import com.binance.accountoauth.enums.ThirdOperatorEnum;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;

@Data
@ApiModel("google登录返回")
public class DoGoogleLoginRet implements Serializable {
    /**
     *
     */
    private static final long serialVersionUID = 4392211032213045581L;

    @ApiModelProperty("三方登陆结果,needRegister需要注册,alreadyRegister已经注册直接登陆")
    private String result;
    private String csrfToken;
    private String token;
    private String currentDeviceId;
    private boolean disable;

    @ApiModelProperty("用户id，为了兼容pnk老系统故返回userId，等pnkweb下线后删除")
    private String userId;
    @ApiModelProperty("用户登陆邮箱")
    private String email;

    @ApiModelProperty("三方注册token")
    private String registerToken;

    // oauth code
    private String code;
    @ApiModelProperty(readOnly = true, notes = "是否非合规国家且有资产")
    private boolean needComplianceAndHashAsset = false;

    @ApiModelProperty(readOnly = true, notes = "非合规国家国家code")
    private String needComplianceCountry;

    @ApiModelProperty(readOnly = true, notes = "非合规国家剩余时间")
    private String needComplianceTimeLeft;

    @ApiModelProperty(readOnly = true, notes = "newDisableLogicFLag是否是新的用户禁用流程")
    private boolean newDisableLogicFLag=false;

    private String refreshToken;

    private String bncLocation;
}