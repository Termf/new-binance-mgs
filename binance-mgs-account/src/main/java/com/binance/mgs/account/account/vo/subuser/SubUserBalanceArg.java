package com.binance.mgs.account.account.vo.subuser;

import com.binance.accountsubuser.vo.enums.FunctionAccountType;
import lombok.Getter;
import lombok.Setter;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.io.Serializable;

@Getter
@Setter
public class SubUserBalanceArg implements Serializable {
    private static final long serialVersionUID = -6144838503849270703L;
    @NotNull
    private FunctionAccountType accountType;
    @NotBlank
    private String coin;
    private String symbol;
    private String email;
    private boolean emailLike;
    private Integer page;
    private Integer rows;
}
