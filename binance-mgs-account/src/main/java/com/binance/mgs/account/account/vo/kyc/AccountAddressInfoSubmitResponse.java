package com.binance.mgs.account.account.vo.kyc;

import lombok.Data;

import java.io.Serializable;

@Data
public class AccountAddressInfoSubmitResponse implements Serializable{

	/**
	 * 
	 */
	private static final long serialVersionUID = -2618857011533395608L;
	
	private int kycType;
	
	private String country;

	private String regionState;
	
	private String regionStateCode;

	private String city;

	private String address;

	private String postalCode;

}
