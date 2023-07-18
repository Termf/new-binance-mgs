package com.binance.mgs.account.marketing.vo;

import com.binance.platform.mgs.base.vo.CommonArg;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;

import javax.validation.constraints.NotNull;

@Getter
@Setter
@ApiModel("上报用户分层数据")
public class UserOperArg extends CommonArg {

    @ApiModelProperty(value = "mature - 老用户(Pro版首页),contact - 新用户有经验(新手版首页),brandnew - 纯小白用户(新手版首页)")
    private ActionTypeEnum actionType;

    public enum ActionTypeEnum{
        MATURE,CONTACT,BRANDNEW;
    }


}
