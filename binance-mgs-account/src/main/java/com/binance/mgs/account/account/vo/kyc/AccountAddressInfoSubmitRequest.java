package com.binance.mgs.account.account.vo.kyc;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.NotNull;
import java.io.Serializable;

@Data
public class AccountAddressInfoSubmitRequest implements Serializable{

	/**
	 * 
	 */
	private static final long serialVersionUID = 4010779545660690327L;

	@NotNull
	private int kycType;
	
	@NotNull
	private String country;

	private String regionState;

	private String city;

	private String address;

	private String postalCode;

	//base64 地址账单图片字符串
	@ApiModelProperty(required = true, notes = "地址认证文件")
	@NotNull
	private String billFile;
	
	@ApiModelProperty(required = true, notes = "地址认证文件名")
	@NotNull
	private String billFileName;
}
