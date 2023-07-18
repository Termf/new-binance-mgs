package com.binance.mgs.account.account.vo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.NotNull;
import java.io.Serializable;

/**
 * Created by yangyang on 2019/7/23.
 */
@Data
@ApiModel("获取下载返佣明细url")
public class CommissionGetDownloadUrlArg implements Serializable {


    @ApiModelProperty(value = "类型1被推荐者返佣2当前用户自己返佣", required = true)
    @NotNull
    private Integer type;
}
