package com.binance.mgs.account.account.vo;

import com.binance.platform.mgs.base.vo.CommonArg;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.validator.constraints.NotEmpty;

@Getter
@Setter
@ApiModel("设置用户默认配置项")
public class SaveUserConfigArg extends CommonArg {

    /**
     * 
     */
    private static final long serialVersionUID = -3994516274432524729L;

    @ApiModelProperty(value = "配置项类型名", required = true)
    @NotEmpty
    private String configType;

    @ApiModelProperty(value = "配置项名称值", required = true)
    @NotEmpty
    private String configName;

}
