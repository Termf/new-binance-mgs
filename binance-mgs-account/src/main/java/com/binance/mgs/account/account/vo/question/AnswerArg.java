package com.binance.mgs.account.account.vo.question;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;

import javax.validation.constraints.NotNull;
import java.util.List;

@ApiModel("答题请求")
@Getter
@Setter
public class AnswerArg {
	@ApiModelProperty("流程的ID")
	@NotNull
	private String flowId;
	@ApiModelProperty("问题id")
	@NotNull
	private Long questionId;
	@ApiModelProperty("答案")
	@NotNull
	private List<String> answers;

	@ApiModelProperty("类型")
	private String flowType;
}
