package com.binance.mgs.account.account.vo.question;

import com.binance.account.vo.reset.response.ResetQuestion;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;

import java.util.LinkedList;
import java.util.List;

@ApiModel("问题以及选项")
@Getter
@Setter
public class QuestionRet {
	@ApiModelProperty("答题剩余秒数")
	private Long timeRemaining;
	@ApiModelProperty("问题数据")
	private List<ResetQuestion> questions = new LinkedList<>();

	@ApiModelProperty("当前是第几次答题")
	private int count;
	@ApiModelProperty("总的最大答题次数")
	private int maxCount;
	@ApiModelProperty("超时失败跳转")
	private String failPath;
}
