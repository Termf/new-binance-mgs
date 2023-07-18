package com.binance.mgs.account.account.vo.subuser;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.NotBlank;

@ApiModel("母账户删除子账户Request")
@Data
public class DeleteSubUserArg {

    @ApiModelProperty("子账户邮箱")
    @NotBlank
    private String subUserEmail;
}
