package com.binance.mgs.account.account.vo.accountMerge;

import javax.validation.constraints.NotNull;

import org.apache.commons.lang3.StringUtils;
import org.hibernate.validator.constraints.Length;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

@ApiModel("SendEmailVerifyCodeForMergeArg")
@Data
@EqualsAndHashCode(callSuper = false)
public class SendEmailVerifyCodeForMergeArg {

    @ApiModelProperty(required = true, value = "email")
    @Length(max = 255)
    @NotNull
    private String email;

    public String getEmail() {
        return StringUtils.trimToEmpty(email).toLowerCase();
    }

}
