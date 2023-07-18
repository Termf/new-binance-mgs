package com.binance.mgs.account.authcenter.vo;

import com.binance.platform.mgs.business.captcha.vo.ValidateCodeArg;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.NotBlank;

/**
 * Description:
 *
 * @author alven
 * @since 2023/4/6
 */
@Data
@ApiModel("RefreshAccessTokenMfaReq")
public class RefreshAccessTokenMfaReq {
    @ApiModelProperty("用来续期登录态的token")
    @NotBlank
    private String refreshToken;
}
