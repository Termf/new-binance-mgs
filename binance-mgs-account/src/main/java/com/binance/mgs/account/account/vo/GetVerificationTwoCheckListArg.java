package com.binance.mgs.account.account.vo;

import com.binance.account.vo.security.enums.BizSceneEnum;
import com.binance.platform.mgs.base.vo.CommonArg;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.validation.constraints.NotNull;

/**
 * @Author: mingming.sheng
 * @Date: 2020/4/14 8:17 下午
 */
@Data
@EqualsAndHashCode(callSuper = false)
public class GetVerificationTwoCheckListArg extends CommonArg {
    private static final long serialVersionUID = 4409911670938433435L;

    @ApiModelProperty("业务场景")
    @NotNull
    private BizSceneEnum bizScene;

    @ApiModelProperty("流程Id-新邮箱验证场景需要")
    private String flowId;
}
