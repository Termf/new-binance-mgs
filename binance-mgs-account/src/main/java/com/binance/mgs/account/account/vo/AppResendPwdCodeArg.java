package com.binance.mgs.account.account.vo;

import com.binance.platform.mgs.base.vo.CommonArg;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.apache.commons.lang3.StringUtils;
import org.hibernate.validator.constraints.NotBlank;

@ApiModel("App重新发送验证码")
@Data
@EqualsAndHashCode(callSuper = false)
public class AppResendPwdCodeArg extends CommonArg {

    /**
     *
     */
    private static final long serialVersionUID = 4688543854410183951L;
    @ApiModelProperty(value = "邮箱", required = true)
    @NotBlank
    private String email;

    public String getEmail() {
        return StringUtils.trimToEmpty(email).toLowerCase();
    }
}
