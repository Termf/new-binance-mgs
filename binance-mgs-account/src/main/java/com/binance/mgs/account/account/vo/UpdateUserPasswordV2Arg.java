package com.binance.mgs.account.account.vo;

import com.binance.master.validator.groups.Edit;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.hibernate.validator.constraints.NotEmpty;

@ApiModel("更新密码")
@Data
@EqualsAndHashCode(callSuper = false)
public class UpdateUserPasswordV2Arg extends MultiCodeVerifyArg {
    private static final long serialVersionUID = -1246453476541663683L;

    @ApiModelProperty(required = true, notes = "原始密码")
    @NotEmpty(groups = Edit.class)
    private String oldPassword;

    @ApiModelProperty(required = true, notes = "新的密码")
    @NotEmpty(groups = Edit.class)
    private String newPassword;

    @ApiModelProperty(required = true, notes = "原始Safe密码")
    @NotEmpty(groups = Edit.class)
    private String oldSafePassword;

    @ApiModelProperty(required = true, notes = "新的Safe密码")
    @NotEmpty(groups = Edit.class)
    private String newSafePassword;
}
