package com.binance.mgs.account.security.vo;

import com.binance.platform.mgs.business.captcha.vo.ValidateCodeArg;
import io.swagger.annotations.ApiModel;
import lombok.Data;

@Data
@ApiModel(value = "SecurityPassChallengeArg")
public class SecurityPassChallengeArg extends ValidateCodeArg {

}
