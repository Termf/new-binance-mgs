package com.binance.mgs.account.account.vo.subuser;

import com.binance.master.utils.DateUtils;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.lang3.StringUtils;
import org.springframework.format.annotation.DateTimeFormat;

import javax.validation.constraints.Max;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.util.Date;
import java.util.List;

/**
 * @author dana.d
 */
@ApiModel("子账户充值列表request")
@Getter
public class QuerySubUserDepositListArg {

    @NotBlank
    @Setter
    private String subUserEmail;

    @ApiModelProperty(notes = "txId")
    @Setter
    private String txId;

    @ApiModelProperty(value = "开始时间")
    @Setter
    @NotNull
    @DateTimeFormat(pattern = DateUtils.DETAILED_NUMBER_PATTERN)
    private Date startTime;

    @ApiModelProperty(value = "结束时间")
    @Setter
    @NotNull
    @DateTimeFormat(pattern = DateUtils.DETAILED_NUMBER_PATTERN)
    private Date endTime;

    @ApiModelProperty(value = "币种")
    private String coin;

    @ApiModelProperty(value = "状态")
    @Setter
    private Integer status;

    @ApiModelProperty(notes = "多个状态查询")
    @Setter
    private List<Integer> statusArray;

    @ApiModelProperty(value = "offset")
    @NotNull
    @Setter
    private Integer offset;

    @ApiModelProperty(value = "limit")
    @NotNull
    @Setter
    @Max(200)
    private Integer limit;

    public void setCoin(String coin) {
        this.coin = StringUtils.trimToNull(coin);
    }
}
