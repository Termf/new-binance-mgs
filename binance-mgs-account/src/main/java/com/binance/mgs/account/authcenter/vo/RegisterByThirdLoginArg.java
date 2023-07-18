package com.binance.mgs.account.authcenter.vo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.validation.constraints.NotBlank;

@ApiModel("三方登陆by注册参数")
@Data
@EqualsAndHashCode(callSuper = false)
public class RegisterByThirdLoginArg {
    @NotBlank
    private String registerToken;

    @ApiModelProperty(required = false, notes = "用户居住国家（前台勾选）")
    @NotBlank
    private String residentCountry;

    @ApiModelProperty(required = false, notes = "是否订阅邮件运营通知")
    private Boolean isEmailPromote;

    @ApiModelProperty(value = "安全验证sessionId")
    @NotBlank
    private String sessionId;

    @ApiModelProperty(required = false, notes = "推荐人")
    private String agentId;

    @ApiModelProperty(required = false, notes = "是否是期货一键开户流程")
    private Boolean isFastCreatFuturesAccountProcess=false;

    @ApiModelProperty("期货返佣推荐码")
    private String futuresReferalCode;
}