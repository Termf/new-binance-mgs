package com.binance.mgs.account.account.vo.reset;

import com.binance.master.commons.ToString;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;

/**
 * @author liliang1
 * @date 2019-02-15 12:04
 */
@ApiModel("答题结果信息")
@Setter
@Getter
public class ResetAnswerQuestionRet extends ToString {

    private static final long serialVersionUID = -5655742286245930516L;

    @ApiModelProperty("是否成功处理")
    private Boolean success;

    @ApiModelProperty("错误信息描述语")
    private String message;

    @ApiModelProperty("错误时的一些特定状态信息，与前端约定")
    private String status;

    @ApiModelProperty("是否回答问题通过")
    private Boolean pass;

    @ApiModelProperty("用户是否被锁定")
    private Boolean lock;

}
