package com.binance.mgs.account.account.vo;

import com.binance.platform.mgs.base.vo.CommonArg;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.validator.constraints.NotBlank;

@Getter
@Setter
@ApiModel("设置昵称")
public class UpdateNickNameArg extends CommonArg {
    private static final long serialVersionUID = 468351637170493910L;
    @ApiModelProperty("昵称")
    @NotBlank
    private String nickName;

}
