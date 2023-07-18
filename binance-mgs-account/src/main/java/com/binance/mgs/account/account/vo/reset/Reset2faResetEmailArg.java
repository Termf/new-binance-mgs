package com.binance.mgs.account.account.vo.reset;

import com.binance.master.commons.ToString;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

@Data
public class Reset2faResetEmailArg extends ToString {

    private static final long serialVersionUID = 1151574681826922354L;

    @ApiModelProperty("重发重置邮件的类型")
    private String type;
}
