package com.binance.mgs.account.account.vo.reset;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.NotNull;
import java.io.Serializable;
import java.util.List;

@Data
public class ResetAnswerQuestionArg implements Serializable {
    private static final long serialVersionUID = 8228724770766545937L;

    @ApiModelProperty("重置流程的ID")
    @NotNull
    private String resetId;
    @ApiModelProperty("问题id")
    @NotNull
    private Long questionId;
    @ApiModelProperty("答案")
    @NotNull
    private List<String> answers;
}
