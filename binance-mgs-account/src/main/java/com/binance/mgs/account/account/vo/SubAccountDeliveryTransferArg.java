package com.binance.mgs.account.account.vo;

import com.binance.platform.mgs.base.vo.CommonArg;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.hibernate.validator.constraints.Length;
import org.hibernate.validator.constraints.Range;

import javax.validation.constraints.DecimalMin;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.math.BigDecimal;

@ApiModel("delivery划转")
@Data
@EqualsAndHashCode(callSuper = false)
public class SubAccountDeliveryTransferArg extends CommonArg {
    private static final long serialVersionUID = 2595088303074381781L;

    @NotBlank(message = "email is not null")
    private String email;

    @NotBlank
    @Length(max = 50)
    private String asset;

    @ApiModelProperty(required = true, value = "划转金额")
    @NotNull
    @DecimalMin(value = "0.0", inclusive = false)
    private BigDecimal amount;

    @ApiModelProperty(required = true, value = "1-main to delivery 2-delivery to main")
    @NotNull
    @Range(min = 1, max = 2)
    private Integer type;
}
