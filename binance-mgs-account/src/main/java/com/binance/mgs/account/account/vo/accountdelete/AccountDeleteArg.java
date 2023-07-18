package com.binance.mgs.account.account.vo.accountdelete;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import org.hibernate.validator.constraints.Length;

import javax.validation.constraints.NotBlank;
import java.io.Serializable;

/**
 * @author rudy.c
 * @date 2022-08-01 19:50
 */
@ApiModel("AccountDeleteArg")
@Data
public class AccountDeleteArg implements Serializable {
    private static final long serialVersionUID = 1L;
    @ApiModelProperty("删除原因")
    @Length(max = 255)
    @NotBlank
    private String reason;
    @ApiModelProperty("是否放弃资产")
    private Boolean giveUpAsset;
    @ApiModelProperty("2fa verifyToken,没有接入MFA才需要")
    private String verifyToken;
}
