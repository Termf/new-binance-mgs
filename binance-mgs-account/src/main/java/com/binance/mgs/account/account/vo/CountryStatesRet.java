package com.binance.mgs.account.account.vo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;

@Data
@ApiModel("国家州代码表")
public class CountryStatesRet implements Serializable {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = -811244064849098673L;
	@ApiModelProperty("国家代码")
	private String code;
	@ApiModelProperty("州代码")
	private String stateCode;
	@ApiModelProperty("英文描述")
	private String en;
	@ApiModelProperty("中文描述")
	private String cn;
	@ApiModelProperty("国籍")
	private String nationality;
	@ApiModelProperty("是否有效")
	private boolean enable;

	
}
