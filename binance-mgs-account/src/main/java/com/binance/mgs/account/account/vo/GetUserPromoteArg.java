package com.binance.mgs.account.account.vo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import java.io.Serializable;

/**
 * Created by yangyang on 2019/7/12.
 */
@Data
@ApiModel("获取被推荐者emails")
public class GetUserPromoteArg implements Serializable {

    @ApiModelProperty(value = "页数", required = true)
    @NotNull
    @Min(value = 1, message = "min value is 1")
    private Integer page;

    @ApiModelProperty(value = "每页行数", required = true)
    @NotNull
    @Max(value = 100, message = "max value is 100")
    @Min(value = 1, message = "min value is 1")
    private Integer rows;

}
