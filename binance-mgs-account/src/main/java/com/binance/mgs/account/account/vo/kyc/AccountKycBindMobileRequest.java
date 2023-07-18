package com.binance.mgs.account.account.vo.kyc;

import lombok.Data;

import java.io.Serializable;

@Data
public class AccountKycBindMobileRequest implements Serializable{

	/**
	 * 
	 */
	private static final long serialVersionUID = -2526676521414457453L;
	
	private int kycType;
	
	private String mobile;
	
	private String mobileCode; 
	
	private String smsCode;

}
