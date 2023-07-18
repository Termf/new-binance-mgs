package com.binance.mgs.account.account.vo.reset;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Data
public class ResetAnswerRet implements Serializable {
    private static final long serialVersionUID = -8089362571379605261L;
	@ApiModelProperty("答题结束,标记答题是怎么结束的")
	private MgsCompleteStatus answerComplete;

	@ApiModel("2FA答题状态枚举")
	public static enum MgsCompleteStatus {
		@ApiModelProperty("当前答题完成,请继续")
		OK, @ApiModelProperty("回答完毕,但是不正确")
		Fail, @ApiModelProperty("答题超时")
		TimeOut, @ApiModelProperty("答题成功")
		Success;

		private final static Map<String, MgsCompleteStatus> map = new ConcurrentHashMap<>(
				MgsCompleteStatus.values().length);

		static {
			for (MgsCompleteStatus e : MgsCompleteStatus.values()) {
				map.put(e.name(), e);
			}
		}

		public static MgsCompleteStatus ConvertFromStr(String name) {
			return map.get(name);
		}
	}
}
