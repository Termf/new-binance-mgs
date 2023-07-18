package com.binance.mgs.account.account.vo.subuser;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

/**
 * @author sean w
 * @date 2022/11/1
 **/
@Data
public class ManagerSubUserFeeDailyArg {
    @NotBlank
    private String email;
    @NotNull
    @Min(value = 1, message = "页数最小为1")
    private Integer page;
    @NotNull
    @Min(value = 1, message = "最小查询数量为1")
    private Integer limit;

    private Long startTime;

    private Long endTime;

    @ApiModelProperty("排序方式 0:Period;1:totalBalance;2:fee;3:amount 不传值默认按时间排序")
    private int sortByField=0;

    @ApiModelProperty("升序还是降序:传值 1:升序; 0:降序 不传值默认降序")
    private int sortDirection=0;
}
