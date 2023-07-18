package com.binance.mgs.account.account.vo.subuser;

import com.binance.commission.vo.user.SubUserTradingVolumeVo;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * @author sean w
 * @date 2023/2/9
 **/
@Data
public class SubUserTradeRecent30RetV2 {

    @ApiModelProperty("子账户id")
    private Long subUserId;

    @ApiModelProperty("子账户邮箱")
    private String subUserEmail;

    @ApiModelProperty("子账户是否被母账户启用")
    private Boolean isSubUserEnabled;

    @ApiModelProperty("最近30天现货交易量")
    private BigDecimal recent30Total;

    @ApiModelProperty("最近30天合约交易量")
    private BigDecimal recent30FuturesTotal;

    @ApiModelProperty("最近30天杠杆交易量")
    private BigDecimal recent30MarginTotal;

    @ApiModelProperty("最近1天现货交易量")
    private BigDecimal recent1Total;

    @ApiModelProperty("最近1天合约交易量")
    private BigDecimal recent1FuturesTotal;

    @ApiModelProperty("最近1天杠杆交易量")
    private BigDecimal recent1MarginTotal;

    @ApiModelProperty("最近30天Busd现货交易量")
    private BigDecimal recent30BusdTotal;

    @ApiModelProperty("最近30天Busd合约交易量")
    private BigDecimal recent30BusdFuturesTotal;

    @ApiModelProperty("最近30天Busd杠杆交易量")
    private BigDecimal recent30BusdMarginTotal;

    @ApiModelProperty("最近1天Busd现货交易量")
    private BigDecimal recent1BusdTotal;

    @ApiModelProperty("最近1天Busd合约交易量")
    private BigDecimal recent1BusdFuturesTotal;

    @ApiModelProperty("最近1天Busd杠杆交易量")
    private BigDecimal recent1BusdMarginTotal;

    @ApiModelProperty("交易量明细")
    private List<SubUserTradingVolumeVo> trades;
}
