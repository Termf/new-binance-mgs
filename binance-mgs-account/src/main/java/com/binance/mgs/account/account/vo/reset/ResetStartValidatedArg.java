package com.binance.mgs.account.account.vo.reset;

import com.binance.account.common.enums.UserSecurityResetType;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.NotNull;
import java.io.Serializable;

@ApiModel("重置前置验证请求参数")
@Data
public class ResetStartValidatedArg implements Serializable {
    private static final long serialVersionUID = 6749024336978174369L;

    @ApiModelProperty("重置类型")
    @NotNull
    private UserSecurityResetType type;

}
