package com.binance.mgs.account.account.vo.subuser;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import org.hibernate.validator.constraints.Range;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;

@ApiModel("查询母子账户逐仓账户详情参数")
@Data
public class QueryIsolatedMarginAccountSummaryArg {

    @ApiModelProperty("当前页数")
    @NotNull
    @Min(1)
    private Integer page;

    @ApiModelProperty("每页记录数")
    @NotNull
    @Range(min = 1, max = 10)
    private Integer rows;

    @ApiModelProperty("子账户邮箱")
    private String subUserEmail;

    @ApiModelProperty("子账户状态")
    private String isSubUserEnabled;
}
