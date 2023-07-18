package com.binance.mgs.account.account.vo.subuser;

import com.binance.platform.mgs.base.vo.CommonArg;
import com.binance.platform.mgs.enums.AccountType;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.NotNull;
import java.util.List;

/**
 * 子用户纵览查询参数
 *
 * @author kvicii
 * @date 2021/05/13
 */
@Data
public class ManagerSubUserAssetArg extends CommonArg {

    @ApiModelProperty("托管子账户id")
    @NotNull
    private String managerSubUserEmail;

    @ApiModelProperty("账户类型，如果需要查询特定账户类型余额需要传入该值，不传则查询所有")
    private List<AccountType> accountTypes;

    @ApiModelProperty("是否需要返回账户里详细币种，默认不返回")
    private boolean needBalanceDetail;
}
