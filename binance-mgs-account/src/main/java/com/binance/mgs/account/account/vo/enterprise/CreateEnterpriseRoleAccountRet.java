package com.binance.mgs.account.account.vo.enterprise;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

/**
 * @author dana.d
 */
@Data
@ApiModel
public class CreateEnterpriseRoleAccountRet {
    @ApiModelProperty(readOnly = true, notes = "角色用户id")
    private Long userId;

    @ApiModelProperty(readOnly = true, notes = "账号")
    private String email;
}
