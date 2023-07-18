package com.binance.mgs.account.account.vo.kyc;

import lombok.Data;

import java.io.Serializable;

@Data
public class AccountFillInfoResponse implements Serializable{

	/**
	 * 
	 */
	private static final long serialVersionUID = 6004343559222182197L;
	
	
	private AccountBaseInfoResponse base;
	
	private AccountBaseInfoResponse address;

}
