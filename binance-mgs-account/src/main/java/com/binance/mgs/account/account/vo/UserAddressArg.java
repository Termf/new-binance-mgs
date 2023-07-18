package com.binance.mgs.account.account.vo;

import com.binance.platform.mgs.base.vo.CommonArg;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;

import javax.validation.constraints.NotNull;

@Getter
@Setter
@ApiModel("地址认证信息")
public class UserAddressArg extends CommonArg {

    /**
     * 
     */
    private static final long serialVersionUID = -8643497188625839797L;

    @ApiModelProperty(required = true, notes = "地址认证文件")
    @NotNull
    private byte[] uploadFile;

    @ApiModelProperty(required = true, notes = "地址认证文件名")
    @NotNull
    private String originalFileName;

    @ApiModelProperty(notes = "街道地址")
    private String street;

    @ApiModelProperty(notes = "国家")
    private String country;

    @ApiModelProperty(notes = "城市")
    private String city;

    @ApiModelProperty(notes = "邮编")
    private String postalCode;
}
