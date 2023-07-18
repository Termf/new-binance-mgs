package com.binance.mgs.account.account.vo.marginRelated;

import com.binance.futurestreamer.api.response.order.OpenOrderVo;
import com.binance.margin.api.bookkeeper.dto.MgsRepayHistoryDto;
import com.binance.margin.api.bookkeeper.enums.RepayStatus;
import com.binance.mgs.account.account.vo.future.OpenOrderRet;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;

import java.math.BigDecimal;

/**
 * @author sean w
 * @date 2021/10/9
 **/
@Data
@ApiModel("还款历史返回 response")
public class RepayHistoryResponseRet {

    @ApiModelProperty("还款时间")
    private Long timestamp;

    @ApiModelProperty("资金名称")
    private String asset;

    @ApiModelProperty("还款金额")
    private BigDecimal amount;

    @ApiModelProperty("偿还本金")
    private BigDecimal principal;

    @ApiModelProperty("偿还利息")
    private BigDecimal interest;

    @ApiModelProperty("状态")
    private RepayStatus status;

    @ApiModelProperty("还款类型 AUTO or MANUAL")
    private String repayType;

    public static RepayHistoryResponseRet of(MgsRepayHistoryDto source) {
        RepayHistoryResponseRet ret = new RepayHistoryResponseRet();
        BeanUtils.copyProperties(source, ret);
        return ret;
    }
}
