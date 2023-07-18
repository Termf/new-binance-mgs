package com.binance.mgs.account.authcenter.vo;

import com.binance.platform.mgs.base.vo.CommonArg;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.hibernate.validator.constraints.NotBlank;

@ApiModel
@Data
@EqualsAndHashCode(callSuper = false)
public class LogoutByClientTypeArg extends CommonArg {
    private static final long serialVersionUID = -6959090927713151737L;
    @ApiModelProperty("设备类型")
    @NotBlank
    private String clientType;
}
