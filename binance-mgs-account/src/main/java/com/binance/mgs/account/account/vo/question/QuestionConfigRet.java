package com.binance.mgs.account.account.vo.question;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;

@ApiModel("问题配置返回")
@Getter
@Setter
public class QuestionConfigRet {
	@ApiModelProperty("当前第几次答题")
    private int count;

    @ApiModelProperty("总答题次数")
    private int maxCount;

    @ApiModelProperty("业务流程超时时间，mins")
    private long timeout;
    
    @ApiModelProperty("成功跳转路径")
    private String successPath;
    
    @ApiModelProperty("失败跳转路径")
    private String failPath;
}
