package com.binance.mgs.account.account.vo.question;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;

import javax.validation.constraints.NotNull;

@ApiModel("问题配置请求参数")
@Getter
@Setter
public class QuestionConfigArg {
	@ApiModelProperty("业务流程号")
    @NotNull
    private String flowId;

    @ApiModelProperty("业务类型")
    @NotNull
    private String flowType;
}
