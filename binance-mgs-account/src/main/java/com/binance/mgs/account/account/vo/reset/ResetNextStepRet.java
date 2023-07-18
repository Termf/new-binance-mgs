package com.binance.mgs.account.account.vo.reset;

import com.binance.account.common.enums.ResetNextStep;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;


@Data
public class ResetNextStepRet implements Serializable {
    private static final long serialVersionUID = -6013643204548555563L;

    @ApiModelProperty("下一步")
    private ResetNextStep nextStep;

    @ApiModelProperty("流水号")
    private String transId;

    @ApiModelProperty("重置类型")
    private String type;

    @ApiModelProperty("请求id")
    private String requestId;

    @ApiModelProperty("当下一步是上传时并且该值存在时，可以直接上传，如果下一步是上传但该值不存在，则提示用户从邮件链接去上传")
    private String uploadUrl;

    /**
     * 是否还有下一笔流程（主要用在review状态下判断是否还有后续流程，还是只需要等待最后的审核）
     */
    @ApiModelProperty("是否还有下一笔流程（主要用在review状态下判断是否还有后续流程，还是只需要等待最后的审核）")
    private boolean haveNext;

    @ApiModelProperty("答题环节下剩余的答题次数")
    private Integer answerCount;

}
