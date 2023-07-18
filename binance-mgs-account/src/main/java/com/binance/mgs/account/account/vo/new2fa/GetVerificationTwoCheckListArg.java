package com.binance.mgs.account.account.vo.new2fa;

import javax.validation.constraints.NotNull;

import com.binance.account2fa.enums.BizSceneEnum;
import com.binance.platform.mgs.base.vo.CommonArg;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.Map;

@Data
@EqualsAndHashCode(callSuper = false)
public class GetVerificationTwoCheckListArg extends CommonArg {

    private static final long serialVersionUID = 5996862168857440463L;
    @ApiModelProperty("业务场景")
    @NotNull
    private String bizScene;

    @ApiModelProperty("流程Id-新邮箱验证场景需要")
    private String flowId;

    @ApiModelProperty("业务透传字段")
    private Map<String, Object> placeMap;
}
