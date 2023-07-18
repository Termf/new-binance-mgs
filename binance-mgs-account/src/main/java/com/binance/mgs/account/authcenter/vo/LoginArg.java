package com.binance.mgs.account.authcenter.vo;

import com.binance.master.enums.AuthTypeEnum;
import com.binance.master.validator.groups.Auth;
import com.binance.master.validator.groups.Select;
import com.binance.master.validator.regexp.Regexp;
import com.binance.platform.mgs.business.captcha.vo.ValidateCodeArg;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.apache.commons.lang3.StringUtils;
import org.hibernate.validator.constraints.Email;
import org.hibernate.validator.constraints.NotEmpty;

import javax.validation.constraints.NotNull;

@ApiModel("登录参数")
@Data
@EqualsAndHashCode(callSuper = false)
public class LoginArg extends ValidateCodeArg {

    /**
     * 
     */
    private static final long serialVersionUID = -7260502467795852399L;

    @ApiModelProperty(required = true, notes = "账号")
    @NotEmpty(groups = {Select.class, Auth.class})
    @Email(groups = {Select.class, Auth.class}, regexp = Regexp.LOGIN_EMAIL)
    private String email;

    @ApiModelProperty(required = false, notes = "密码")
    @NotEmpty(groups = Select.class)
    private String password;

    @ApiModelProperty(required = true, notes = "新算法的密码")
    private String safePassword;

    @ApiModelProperty(required = false, notes = "验证类型")
    @NotNull(groups = Auth.class)
    private AuthTypeEnum authType;

    @ApiModelProperty(required = false, notes = "2次验证码")
    @NotEmpty(groups = Auth.class)
    private String verifyCode;
//    @ApiModelProperty(required = false, notes = "设备信息 BASE64编码")
//    private String deviceInfo;

    @ApiModelProperty(required = false, notes = "是否走新的登录流程")
    private Boolean isNewLoginProcess=false;

    @ApiModelProperty(value = "用于登陆绑定三方账户")
    private String registerToken;

    @ApiModelProperty(value = "登陆涞源，如果是ThreeParty则是三方登陆")
    private String threePartySource;

    public void setEmail(String email) {
        this.email = StringUtils.trimToEmpty(email).toLowerCase();
    }

    public void setPassword(String password) {
        this.password = password == null ? null : password.trim();
    }
}
