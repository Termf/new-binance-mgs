package com.binance.mgs.account.account.vo.subuser;

import com.binance.commission.vo.user.SubUserTradeNumberVo;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@ApiModel("子账户交易量信息")
@Data
public class SubUserTradeRecent30Ret {
    @ApiModelProperty("子账户id")
    private Long subUserId;

    @ApiModelProperty("子账户邮箱")
    private String subUserEmail;

    @ApiModelProperty(readOnly = true, notes = "子账户是否被母账户启用")
    private Boolean isSubUserEnabled;

    @ApiModelProperty("最近30天现货交易量")
    private BigDecimal recent30Total;

    @ApiModelProperty("最近30天合约交易量")
    private BigDecimal recent30FuturesTotal;

    @ApiModelProperty("最近1天现货交易量")
    private BigDecimal recent1Total;

    @ApiModelProperty("最近1天合约交易量")
    private BigDecimal recent1FuturesTotal;

    @ApiModelProperty("最近30天Busd现货交易量")
    private BigDecimal recent30BusdTotal;

    @ApiModelProperty("最近30天Busd合约交易量")
    private BigDecimal recent30BusdFuturesTotal;

    @ApiModelProperty("最近1天Busd现货交易量")
    private BigDecimal recent1BusdTotal;

    @ApiModelProperty("最近1天Busd合约交易量")
    private BigDecimal recent1BusdFuturesTotal;

    @ApiModelProperty("交易量明细")
    private List<SubUserTradeNumberVo> trades ;
}
