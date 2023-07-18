package com.binance.mgs.account.account.vo.reset;

import com.binance.account.common.enums.UserSecurityResetType;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.NotNull;
import java.io.Serializable;

@Data
public class ResetOpenEmailArg implements Serializable {
    private static final long serialVersionUID = 7377614180285900623L;

    @ApiModelProperty("邮件链接中的requestId")
    @NotNull
    private String requestId;

    @ApiModelProperty("流水号")
    @NotNull
    private String transId;

    @ApiModelProperty("业务类型")
    @NotNull
    private UserSecurityResetType type;
}
