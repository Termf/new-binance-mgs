package com.binance.mgs.account.account.vo.subuser;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.NotEmpty;

@ApiModel("子账户交易统计实体")
@Data
public class SubUserTradeRecent30Arg {

    @ApiModelProperty("子账户邮箱")
    @NotEmpty
    private String subUserEmail;
}
