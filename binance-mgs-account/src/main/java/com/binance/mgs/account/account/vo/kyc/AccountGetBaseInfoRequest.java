package com.binance.mgs.account.account.vo.kyc;

import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;

@Getter
@Setter
public class AccountGetBaseInfoRequest implements Serializable{

	/**
	 * 
	 */
	private static final long serialVersionUID = -947675662372978296L;
	
	private String fillType;

}
