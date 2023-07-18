package com.binance.mgs.account.account.vo.marginRelated;

import com.binance.margin.api.bookkeeper.dto.MgsBorrowHistoryDto;
import com.binance.margin.api.bookkeeper.enums.BorrowStatus;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import org.springframework.beans.BeanUtils;

import java.math.BigDecimal;

/**
 * @author sean w
 * @date 2021/10/9
 **/
@Data
@ApiModel("借款历史返回 response")
public class BorrowHistoryResponseRet {

    @ApiModelProperty("借款时间")
    private Long timestamp;

    @ApiModelProperty("资金名称")
    private String asset;

    @ApiModelProperty("借款金额")
    private BigDecimal principal;

    @ApiModelProperty("状态")
    private BorrowStatus status;

    @ApiModelProperty("借款类型，AUTO自动借款 MANUAL人工借款")
    private String borrowType;

    public static BorrowHistoryResponseRet of(MgsBorrowHistoryDto source) {
        BorrowHistoryResponseRet ret = new BorrowHistoryResponseRet();
        BeanUtils.copyProperties(source, ret);
        return ret;
    }
}
