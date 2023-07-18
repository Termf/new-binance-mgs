package com.binance.mgs.account.account.vo.subuser;

import com.binance.platform.mgs.base.vo.CommonPageRet;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

@Data
public class CommMerchantSubUserRet<T> extends CommonPageRet<T> {
    @ApiModelProperty("子账户创建的最大个数")
    private long maxSubUserNum;
}
