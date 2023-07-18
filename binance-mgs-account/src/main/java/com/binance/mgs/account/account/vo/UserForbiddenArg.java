package com.binance.mgs.account.account.vo;

import com.binance.platform.mgs.base.vo.CommonArg;
import io.swagger.annotations.ApiModel;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.validator.constraints.NotBlank;

@Getter
@Setter
@ApiModel("通过邮件禁用用户账号")
public class UserForbiddenArg extends CommonArg {

    private static final long serialVersionUID = 4519006554398163456L;
    @NotBlank
    private String userId;
    @NotBlank
    private String verifyCode;

}
