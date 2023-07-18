package com.binance.mgs.account.account.vo.enterprise;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import java.util.List;

@ApiModel("条件角色账户列表Request")
@Getter
@Setter
public class QueryEnterpriseRoleUserArg {

    @ApiModelProperty(required = true, notes = "角色id")
    private List<Long> roleIds;

    @NotNull
    @Min(1)
    private Integer page;

    @NotNull
    @Max(20)
    private Integer rows;
}
