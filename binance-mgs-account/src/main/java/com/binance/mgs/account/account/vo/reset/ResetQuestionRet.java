package com.binance.mgs.account.account.vo.reset;

import com.binance.account.vo.reset.response.ResetQuestion;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;
import java.util.LinkedList;
import java.util.List;

@Data
public class ResetQuestionRet implements Serializable {
	private static final long serialVersionUID = -485217042828069833L;

	@ApiModelProperty("答题剩余秒数")
	private Long timeRemaining;
	@ApiModelProperty("问题数据")
	private List<ResetQuestion> questions = new LinkedList<>();
}
