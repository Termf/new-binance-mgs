package com.binance.mgs.account.account.vo.subuser;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;

import javax.validation.constraints.NotNull;

@ApiModel("条件查询商户子账户列表Request")
@Getter
@Setter
public class QueryCommMerchantSubUserArg {

    @ApiModelProperty(required = false, notes = "子账户邮箱")
    private String email;


    @ApiModelProperty(required = true, notes = "子账户业务类型,参考：com.binance.mgs.account.account.enums.SubUserBizType")
    @NotNull
    private String subUserBizType;

    private Integer page = 1;

    private Integer rows = 20;
}
