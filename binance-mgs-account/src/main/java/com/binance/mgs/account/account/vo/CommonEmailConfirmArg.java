package com.binance.mgs.account.account.vo;

import com.binance.platform.mgs.base.vo.CommonArg;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.hibernate.validator.constraints.NotEmpty;

@ApiModel("邮箱确认")
@Data
@EqualsAndHashCode(callSuper = false)
public class CommonEmailConfirmArg extends CommonArg {


    /**
     * 
     */
    private static final long serialVersionUID = -5946507008970040238L;
    @ApiModelProperty(required = true, notes = "唯一id")
    @NotEmpty
    private String id;

}
