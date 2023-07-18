package com.binance.mgs.account.account.vo.kyc;

import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.lang3.StringUtils;

import java.io.Serializable;

@Setter
@Getter
public class KycNameAndCountryRet implements Serializable {

    private static final long serialVersionUID = -7483651263225867417L;

    @ApiModelProperty("认证类型: 1:个人认证, 2:企业认证 -1:未认证 ")
    private Integer certificateType;

    @ApiModelProperty("如果认证过KYC，返回两位的国家码")
    private String countryCode;

    @ApiModelProperty("kyc认证通过后的名")
    private String firstName = "";

    @ApiModelProperty("kyc认证通过后的姓")
    private String lastName = "";

    public void setFirstName(String firstName) {
        this.firstName = StringUtils.isBlank(firstName) ? "" : firstName;
    }

    public void setLastName(String lastName) {
        this.lastName = StringUtils.isBlank(lastName) ? "" : lastName;
    }
}
