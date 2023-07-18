package com.binance.mgs.account.account.vo.reset;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.NotNull;
import java.io.Serializable;

@Data
public class ResetQuestionMgsArg implements Serializable {
	private static final long serialVersionUID = 9158315532676113454L;

	@ApiModelProperty("重置请求id")
	@NotNull
	private String requestId;

	@ApiModelProperty("重置id,用于校验")
	@NotNull
	private String transId;

	@ApiModelProperty("重置type,用于校验")
	@NotNull
	private String type;
}
