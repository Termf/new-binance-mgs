package com.binance.mgs.account.account.vo.subuser;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.NotNull;

/**
 * Created by Kay.Zhao on 2021/2/3
 */
@ApiModel("删除商户子用户Request")
@Data
public class DeleteCommMerchantSubUserArg {

    @ApiModelProperty("商户子账号userId")
    @NotNull
    private Long subUserId;

}