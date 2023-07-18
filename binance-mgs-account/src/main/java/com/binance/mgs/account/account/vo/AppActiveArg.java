package com.binance.mgs.account.account.vo;

import com.binance.platform.mgs.base.vo.CommonArg;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.apache.commons.lang3.StringUtils;
import org.hibernate.validator.constraints.NotBlank;

@ApiModel("APP验证注册验证码")
@Data
@EqualsAndHashCode(callSuper = false)
public class AppActiveArg extends CommonArg {

    /**
     * 
     */
    private static final long serialVersionUID = 6399747612897464825L;
    @ApiModelProperty(value = "邮箱", required = true)
    @NotBlank
    private String email;
    @ApiModelProperty(value = "发送到邮箱的验证码", required = true)
    @NotBlank
    private String verifyCode;

    public String getEmail() {
        return StringUtils.trimToEmpty(email).toLowerCase();
    }
}
