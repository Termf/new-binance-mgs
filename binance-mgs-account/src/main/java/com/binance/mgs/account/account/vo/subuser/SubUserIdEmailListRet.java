package com.binance.mgs.account.account.vo.subuser;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.util.List;
/**
 * @author Men Huatao (alex.men@binance.com)
 * @date 2021/04/07
 */
@Data
public class SubUserIdEmailListRet {
    @ApiModelProperty("Result list")
    private List<SubUserIdEmailRet> retList;
    @ApiModelProperty("Total number of sub accounts")
    private Long total;
    @ApiModelProperty("Size of a page")
    private Integer pageSize;
}
