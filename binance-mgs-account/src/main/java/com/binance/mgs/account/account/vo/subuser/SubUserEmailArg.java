package com.binance.mgs.account.account.vo.subuser;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.NotEmpty;

/**
 * @author rudy.c
 * @date 2021-09-13 15:55
 */
@Data
public class SubUserEmailArg {
    @ApiModelProperty(value = "子账号邮箱", required = true)
    @NotEmpty
    private String subUserEmail;

    public String getSubUserEmail() {
        if(subUserEmail == null) {
            return subUserEmail;
        }
        return subUserEmail.trim().toLowerCase();
    }
}
