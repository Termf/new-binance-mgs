package com.binance.mgs.account.account.vo;

import com.binance.platform.mgs.base.vo.CommonPageArg;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.Collection;
import java.util.Date;


@ApiModel("查询用户行为日志")
@Data
@EqualsAndHashCode(callSuper = false)
public class QueryUserOperationLogArg extends CommonPageArg {

    @ApiModelProperty("请求时间（查询范围开始）")
    private Date startTime;

    @ApiModelProperty("请求时间（查询范围结束）")
    private Date endTime;

    @ApiModelProperty("status. " +
            "0/null -- all " +
            "1 -- only responseStatus == true" +
            "2 -- only responseStatus == false and failReason is not null")
    private Integer status;


    @ApiModelProperty("operations. 空--查询所有")
    private Collection<String> operations;

}
