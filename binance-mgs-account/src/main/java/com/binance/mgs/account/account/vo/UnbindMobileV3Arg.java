package com.binance.mgs.account.account.vo;

import com.binance.platform.mgs.base.vo.CommonArg;
import io.swagger.annotations.ApiModel;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

@ApiModel("解绑手机unbindMobileV3,接入MFA")
@Getter
@Setter
@EqualsAndHashCode(callSuper = false)
public class UnbindMobileV3Arg extends CommonArg {

}
