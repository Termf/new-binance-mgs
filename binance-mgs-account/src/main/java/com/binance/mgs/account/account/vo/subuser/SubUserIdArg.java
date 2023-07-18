package com.binance.mgs.account.account.vo.subuser;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.NotNull;

/**
 * Created by Kay.Zhao on 2020/10/27
 */
@ApiModel("SubUserIdArg")
@Data
public class SubUserIdArg {
    
    @ApiModelProperty("子账号userId")
    @NotNull
    private Long subUserId;
}
