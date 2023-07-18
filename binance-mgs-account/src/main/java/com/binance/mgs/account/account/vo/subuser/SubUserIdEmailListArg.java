package com.binance.mgs.account.account.vo.subuser;

import com.binance.accountsubuser.vo.enums.FunctionAccountType;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.NotNull;

/**
 * @author Men Huatao (alex.men@binance.com)
 * @date 2021/04/07
 */
@Data
@ApiModel("SubUserIdEmailListArg")
public class SubUserIdEmailListArg {
    @ApiModelProperty("Email pattern")
    private String email;
    @ApiModelProperty("Is enable fuzzy query")
    private Boolean isEmailLike;
    @ApiModelProperty("Account type")
    private FunctionAccountType accountType;
    @NotNull
    @ApiModelProperty("query page, should not null, and the size of page is controlled by the backend")
    private Integer page;
}
