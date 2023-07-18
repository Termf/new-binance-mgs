package com.binance.mgs.account.account.vo.kyc;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;

@Data
public class AccountFaceInitResponse implements Serializable{

	/**
	 * 
	 */
	private static final long serialVersionUID = -8608446261454800588L;
	
	@ApiModelProperty("人脸识别流程标识")
    private String transId;

    @ApiModelProperty("人脸识别类型")
    private String transType;
    
    private int kycType;
    
    private boolean kycFaceSwitch;

}
