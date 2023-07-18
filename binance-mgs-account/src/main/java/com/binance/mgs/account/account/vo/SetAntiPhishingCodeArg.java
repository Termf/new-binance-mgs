package com.binance.mgs.account.account.vo;

import com.binance.master.enums.AuthTypeEnum;
import com.binance.master.validator.regexp.Regexp;
import com.binance.platform.mgs.base.vo.CommonArg;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.hibernate.validator.constraints.Length;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;

@ApiModel("设置钓鱼码")
@Data
@EqualsAndHashCode(callSuper = false)
public class SetAntiPhishingCodeArg extends CommonArg {

    /**
     *
     */
    private static final long serialVersionUID = 2979586239621445005L;

    @ApiModelProperty("防钓鱼码")
    @NotNull
    @Length(min = 4, max = 20)
    @Pattern(regexp = Regexp.PHISHING_CODE_IGNORE,
            message = "${com.binance.master.validator.constraints.phishingCodeIgnore.message}")
    private String antiPhishingCode;

    @ApiModelProperty("2fa验证码")
    private String code;

    @ApiModelProperty("认证标识")
    private AuthTypeEnum authType;
}
