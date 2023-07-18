package com.binance.mgs.account.account.vo.subuser;

import lombok.Data;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;

/**
 * @author sean w
 * @date 2022/11/1
 **/
@Data
public class ManagerSubUserFeeArg {

    @NotNull
    @Min(value = 1, message = "页数最小为1")
    private Integer page;

    @NotNull
    @Min(value = 1, message = "最小查询数量为1")
    private Integer limit;

    private boolean hideWithOutFeeSetting=false;
}
