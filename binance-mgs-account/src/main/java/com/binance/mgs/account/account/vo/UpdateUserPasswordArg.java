package com.binance.mgs.account.account.vo;

import com.binance.master.enums.AuthTypeEnum;
import com.binance.master.validator.groups.Edit;
import com.binance.platform.mgs.base.vo.CommonArg;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.hibernate.validator.constraints.NotEmpty;

@ApiModel("更新密码")
@Data
@EqualsAndHashCode(callSuper = false)
public class UpdateUserPasswordArg extends CommonArg {
    /**
     * 
     */
    private static final long serialVersionUID = -5491911931893210682L;
    @ApiModelProperty(required = true, notes = "新的密码")
    @NotEmpty(groups = Edit.class)
    private String newPassword;

    @ApiModelProperty(required = true, notes = "原始密码")
    @NotEmpty(groups = Edit.class)
    private String oldPassword;


    @ApiModelProperty(required = false, notes = "认证类型")
    private AuthTypeEnum authType;

    @ApiModelProperty(required = false, notes = "2次验证码")
    private String code;
}
