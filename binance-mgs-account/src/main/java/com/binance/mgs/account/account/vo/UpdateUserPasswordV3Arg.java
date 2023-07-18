package com.binance.mgs.account.account.vo;

import com.binance.master.validator.groups.Edit;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.validation.constraints.NotEmpty;

/**
 * Description:
 *
 * @author alven
 * @since 2023/4/26
 */
@ApiModel("更新密码V3")
@Data
@EqualsAndHashCode(callSuper = false)
public class UpdateUserPasswordV3Arg {
    @ApiModelProperty(required = true, notes = "新的Safe密码")
    @NotEmpty
    private String newSafePassword;

    @ApiModelProperty(required = true, notes = "新的密码")
    @NotEmpty
    private String newPassword;
}
