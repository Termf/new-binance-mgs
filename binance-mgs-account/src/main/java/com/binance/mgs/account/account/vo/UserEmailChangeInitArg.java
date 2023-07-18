package com.binance.mgs.account.account.vo;

import io.swagger.annotations.ApiModel;
import lombok.Data;

import javax.validation.constraints.NotNull;

@Data
@ApiModel("初始化流程所需信息")
public class UserEmailChangeInitArg {
    @NotNull
    private Integer availableType;//0： 老邮箱可用，1：老邮箱不可用，2：2fa 方式邮箱

}
