package com.binance.mgs.account.authcenter.vo;

import com.binance.accountoauth.enums.ThirdOperatorEnum;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

@ApiModel("三方注册参数")
@Data
@EqualsAndHashCode(callSuper = false)
public class RegisterByThirdRegisterArg {

    @NotNull
    private ThirdOperatorEnum thirdOperatorEnum;

    private String registerToken;

    @ApiModelProperty(value = "安全验证sessionId")
    @NotBlank
    private String sessionId;

    @ApiModelProperty(required = false, notes = "用户居住国家（前台勾选）")
    private String residentCountry;

    @ApiModelProperty(required = false, notes = "是否订阅邮件运营通知")
    private Boolean isEmailPromote;

    @ApiModelProperty(required = false, notes = "推荐人")
    private String agentId;

    @ApiModelProperty(required = false, notes = "是否是期货一键开户流程")
    private Boolean isFastCreatFuturesAccountProcess=false;

    @ApiModelProperty("期货返佣推荐码")
    private String futuresReferalCode;

    @ApiModelProperty(required = false,name = "utm source")
    private String source;

    @ApiModelProperty("个人或者企业账户,默认个人账户")
    private Boolean isPersonalAccount = true;

    @ApiModelProperty("是否统计agentCodeError")
    private Boolean isStatAgentError = false;

    @ApiModelProperty(required = false, notes = "注册渠道，对应原先的ts字段")
    private String registerChannel;

    @ApiModelProperty("oauth client id")
    private String oauthClientId;

}