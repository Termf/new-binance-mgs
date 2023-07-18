package com.binance.mgs.account.account.vo;

import com.binance.accountsubuser.vo.enums.FunctionAccountType;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import org.hibernate.validator.constraints.Length;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.io.Serializable;

@ApiModel("最大提现额度")
@Data
public class AccountMaxWithdrawArg implements Serializable {

    private static final long serialVersionUID = -5498048439392354687L;
    @NotNull
    private FunctionAccountType accountType;

    @NotBlank(message = "email is not null")
    private String email;

    @NotBlank
    @Length(max = 50)
    private String asset;

    @Length(max = 50)
    private String symbol;
}
