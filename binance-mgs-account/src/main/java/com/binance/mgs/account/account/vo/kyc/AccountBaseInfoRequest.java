package com.binance.mgs.account.account.vo.kyc;

import java.io.Serializable;
import java.util.Date;

import io.swagger.annotations.ApiModel;
import lombok.Data;

@ApiModel("基础信息")
@Data
public class AccountBaseInfoRequest implements Serializable {

	/**
	 *
	 */
	private static final long serialVersionUID = 2262019705908153240L;

	private String status;

	private Date createTime;

	private Date updateTime;

	private String firstName;

	private String middleName;

	private String lastName;

	private byte gender;

	private String birthday;

	private String taxId;

	private String country;

	private String residenceCountry;

	private String regionState;

	private String city;

	private String address;

	private String postalCode;

	private String nationality;

	private String billFile;

	private String companyName;

	private String companyAddress;

	private String contactNumber;

	private String registerName;

	private String registerEmail;

	private int kycType;

	private String flowDefine;

	private String idcardNumber;

	private String tin;

	private String idType;
	// 证件发行者
	private String issuer;
	// 住所街道地址
	private String suburb;
	// 签证国家
	private String countryOfIssue;
}
