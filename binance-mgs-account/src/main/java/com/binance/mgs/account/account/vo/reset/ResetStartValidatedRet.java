package com.binance.mgs.account.account.vo.reset;

import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@NoArgsConstructor
@AllArgsConstructor
@Data
public class ResetStartValidatedRet implements Serializable {
    private static final long serialVersionUID = 5093419766850774441L;

    @ApiModelProperty("请求id")
    private String requestId;

    @ApiModelProperty("保护模式下到答题剩余次数")
    private int protectCount;
}
