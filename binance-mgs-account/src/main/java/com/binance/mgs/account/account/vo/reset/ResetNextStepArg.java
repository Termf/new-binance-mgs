package com.binance.mgs.account.account.vo.reset;

import com.binance.account.common.enums.UserSecurityResetType;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.NotNull;
import java.io.Serializable;

@Data
public class ResetNextStepArg implements Serializable {
    private static final long serialVersionUID = -1239035149341785962L;

    @ApiModelProperty("重置类型")
    @NotNull
    private UserSecurityResetType type;


}
