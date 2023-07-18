package com.binance.mgs.account.account.vo;

import io.swagger.annotations.ApiModel;
import lombok.Data;
import org.hibernate.validator.constraints.NotBlank;

@Data
@ApiModel("用户点击老邮箱链接")
public class UserEmailChangeLinkArg {

    @NotBlank
    private String requestId;

    private String sign;

}
