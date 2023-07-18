package com.binance.mgs.account.account.vo.subuser;

import com.binance.accountsubuser.vo.enums.FunctionAccountType;
import io.swagger.annotations.ApiModel;
import lombok.Data;
import org.hibernate.validator.constraints.Length;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

@ApiModel("ManagerAccountMaxWithdrawArg")
@Data
public class ManagerAccountMaxWithdrawArg  {

    @NotNull
    private FunctionAccountType accountType;

    @NotBlank(message = "email is not null")
    private String managerSubEmail;

    @NotBlank
    @Length(max = 50)
    private String asset;

}
