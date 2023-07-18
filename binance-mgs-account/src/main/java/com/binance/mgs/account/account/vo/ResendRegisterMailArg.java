package com.binance.mgs.account.account.vo;

import com.binance.platform.mgs.business.captcha.vo.ValidateCodeArg;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.lang3.StringUtils;
import org.hibernate.validator.constraints.Length;
import org.hibernate.validator.constraints.NotBlank;

@Getter
@Setter
@ApiModel("重发注册邮件注册")
public class ResendRegisterMailArg extends ValidateCodeArg {

    /**
     * 
     */
    private static final long serialVersionUID = 3941484932746658174L;

    @ApiModelProperty(required = true, notes = "邮箱")
    @NotBlank
    @Length(max = 255)
    private String email;

    @ApiModelProperty(required = false, notes = "是否走新的注册流程")
    private Boolean isNewRegistrationProcess=false;

    public String getEmail() {
        return StringUtils.trimToEmpty(email).toLowerCase();
    }
}
