package com.binance.mgs.account.account.vo.kyc;

import lombok.Data;

import java.io.Serializable;

@Data
public class AccountFaceInitRequest implements Serializable{

	/**
	 * 
	 */
	private static final long serialVersionUID = -8323734326128098188L;
	
	private int kycType;

}
