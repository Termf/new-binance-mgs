package com.binance.mgs.account.account.vo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.validator.constraints.Length;
import org.hibernate.validator.constraints.NotBlank;

import java.io.Serializable;

@ApiModel("企业认证Arg")
@Getter
@Setter
public class CompanyAuthenticationArg implements Serializable {

    private static final long serialVersionUID = 952408291526014494L;

    @ApiModelProperty(value = "公司名称", required = true)
    @NotBlank
    @Length(max = 1024, message = "company name size [1, 1024]")
    private String companyName;

    @ApiModelProperty(value = "公司地址", required = false)
    @Length(min = 0, max = 2048, message = "company address size [0, 2048]")
    private String companyAddress;

    @ApiModelProperty(value = "公司所在国家", required = true)
    @NotBlank
    @Length(max = 128, message = "comapny country size [1, 128]")
    private String companyCountry;

    @ApiModelProperty(value = "申请人名称", required = true)
    @NotBlank
    @Length(max = 128, message = "applyer name size [1, 128]")
    private String applyerName;

    @ApiModelProperty(value = "申请人email", required = true)
//    @Email(regexp = Constant.EMAIL_REGEX, message = "Invalid email ")
    private String applyerEmail;

    @ApiModelProperty(value = "联系方式", required = true)
    @NotBlank
    @Length(max = 128, message = "contact number size [1, 128]")
    private String contactNumber;

}
