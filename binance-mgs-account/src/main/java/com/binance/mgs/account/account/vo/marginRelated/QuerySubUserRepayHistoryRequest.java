package com.binance.mgs.account.account.vo.marginRelated;

import com.binance.margin.isolated.api.repay.enums.RepayStatus;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import org.hibernate.validator.constraints.Length;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;

/**
 * @author sean w
 * @date 2021/9/27
 **/
@Data
@ApiModel("查询子账户还款历史请求")
public class QuerySubUserRepayHistoryRequest {

    @ApiModelProperty(required = true, value = "子账户邮箱")
    private String email;

    @ApiModelProperty("交易对")
    private String symbol;

    @ApiModelProperty("资产名称")
    @Length(max = 10)
    private String asset;

    private RepayStatus status;

    private Long txId;

    private Long startTime;

    private Long endTime;

    @ApiModelProperty("当前页")
    @Min(1)
    private Long current = 1L;

    @Max(100)
    private Long size = 10L;

    @ApiModelProperty("是否查询归档数据")
    private boolean archived = false;

    @ApiModelProperty("是否展示还款类型")
    private boolean needRepayType = false;
}
