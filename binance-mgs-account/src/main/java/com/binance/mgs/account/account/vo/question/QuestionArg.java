package com.binance.mgs.account.account.vo.question;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;

import javax.validation.constraints.NotNull;

@ApiModel("问题以及选项请求参数")
@Getter
@Setter
public class QuestionArg {
	@ApiModelProperty("业务流程号")
    @NotNull
    private String flowId;

    @ApiModelProperty("业务类型")
    @NotNull
    private String flowType;
}
