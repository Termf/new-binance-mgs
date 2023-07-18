package com.binance.mgs.account.account.vo.margin;

import com.binance.platform.mgs.base.vo.CommonArg;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.NotEmpty;

@Data
@ApiModel
public class IsolatedMarginSummaryQueryArg extends CommonArg {


    private static final long serialVersionUID = 4931188091176207395L;

    @ApiModelProperty(required = true, name = "子账号邮箱")
    @NotEmpty
    private String email;
}
