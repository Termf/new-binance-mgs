package com.binance.mgs.account.account.vo.question;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;

@ApiModel("答题返回")
@Getter
@Setter
public class AnswerRet {
	@ApiModelProperty("答题结果状态")
	private String status;

	@ApiModelProperty("当前答题的次数")
	private int count;

	@ApiModelProperty("当前答题的允许最大次数")
	private int maxCount;

	@ApiModelProperty("当成功或者失败后当跳转地址")
	private String gotoPath;

	@Override
	public String toString() {
		return "AnswerRet [status=" + status + ", count=" + count + ", maxCount=" + maxCount + ", gotoPath=" + gotoPath
				+ "]";
	}
}
