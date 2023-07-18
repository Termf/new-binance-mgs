package com.binance.mgs.account.account.vo.accountdelete;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * @author rudy.c
 * @date 2022-08-01 19:52
 */
@ApiModel("AccountDeleteCheckRet")
@Data
public class AccountDeleteCheckRet implements Serializable {
    private static final long serialVersionUID = 1L;
    @ApiModelProperty("是否可以删除")
    private boolean canDelete;
    @ApiModelProperty("btc资产")
    private BigDecimal btcAsset;
    @ApiModelProperty("NFT资产数量")
    private Integer nftNumber;
}
