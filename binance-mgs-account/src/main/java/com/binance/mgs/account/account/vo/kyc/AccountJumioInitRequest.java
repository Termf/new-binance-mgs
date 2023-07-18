package com.binance.mgs.account.account.vo.kyc;

import lombok.Data;

import java.io.Serializable;

@Data
public class AccountJumioInitRequest implements Serializable{

	/**
	 * 
	 */
	private static final long serialVersionUID = -7636046634862999004L;
	
	private int kycType;
	
	private boolean lockJumio;
    
}
