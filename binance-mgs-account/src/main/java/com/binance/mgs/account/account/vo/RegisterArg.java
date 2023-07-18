package com.binance.mgs.account.account.vo;

import com.binance.master.validator.groups.Add;
import com.binance.platform.mgs.business.captcha.vo.ValidateCodeArg;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.lang3.StringUtils;
import org.hibernate.validator.constraints.Email;
import org.hibernate.validator.constraints.NotBlank;
import org.hibernate.validator.constraints.NotEmpty;

@Getter
@Setter
@ApiModel("用户注册")
public class RegisterArg extends ValidateCodeArg {

    /**
     * 
     */
    private static final long serialVersionUID = 3941484932746658174L;

    @ApiModelProperty(required = true, notes = "邮箱")
    @NotBlank(groups = Add.class)
    @Email(groups = Add.class)
    private String email;
    @ApiModelProperty(required = true, notes = "密码")
    @NotEmpty(groups = Add.class)
    private String password;

    @ApiModelProperty(required = true, notes = "新算法的密码")
    private String safePassword;

    @ApiModelProperty(required = false, notes = "推荐人")
    private String agentId;
//    @ApiModelProperty(required = false, notes = "设备信息 BASE64编码")
//    private String deviceInfo;
    @ApiModelProperty(required = false, notes = "注册渠道，对应原先的ts字段")
    private String registerChannel;
    @ApiModelProperty(required = false, notes = "是否走新的注册流程")

    private Boolean isNewRegistrationProcess=false;


    @ApiModelProperty(required = false, notes = "是否是期货一键开户流程")
    private Boolean isFastCreatFuturesAccountProcess=false;

    @ApiModelProperty("期货返佣推荐码")
    private String futuresReferalCode;


    @ApiModelProperty(required = false, notes = "是否检查ip")
    private Boolean checkIp=true;

    public String getEmail() {
        return StringUtils.trimToEmpty(email).toLowerCase();
    }
}
