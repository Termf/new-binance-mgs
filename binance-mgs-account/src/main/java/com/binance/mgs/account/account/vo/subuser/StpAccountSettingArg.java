package com.binance.mgs.account.account.vo.subuser;

import com.binance.accountsubuser.vo.enums.ParentStpAccountSettingType;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.NotNull;

/**
 * @author sean w
 * @date 2023/3/22
 **/
@Data
public class StpAccountSettingArg {

    @ApiModelProperty(required = true, notes = "STP类型")
    @NotNull
    private ParentStpAccountSettingType parentStpAccountSettingType;
}
