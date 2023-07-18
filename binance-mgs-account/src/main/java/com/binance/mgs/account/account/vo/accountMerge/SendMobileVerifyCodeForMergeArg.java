package com.binance.mgs.account.account.vo.accountMerge;

import javax.validation.constraints.NotNull;

import org.hibernate.validator.constraints.Length;

import com.binance.account.vo.security.enums.BizSceneEnum;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

@ApiModel("SendMobileVerifyCodeForMergeArg")
@Data
@EqualsAndHashCode(callSuper = false)
public class SendMobileVerifyCodeForMergeArg {

    @ApiModelProperty("手机code")
    @NotNull
    @Length(max = 10)
    private String mobileCode;
    
    @ApiModelProperty("手机号")
    @NotNull
    @Length(max = 50)
    private String mobile;

    @ApiModelProperty(required = false, notes = "userChannel")
    private String userChannel;

}
