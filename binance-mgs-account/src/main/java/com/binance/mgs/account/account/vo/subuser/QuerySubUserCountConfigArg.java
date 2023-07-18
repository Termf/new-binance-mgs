package com.binance.mgs.account.account.vo.subuser;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import org.hibernate.validator.constraints.Length;

/**
 * @author Men Huatao (alex.men@binance.com)
 * @date 2021/5/17
 */
@Data
@ApiModel
public class QuerySubUserCountConfigArg {
    @ApiModelProperty("类型 SPOT MARGIN FUTURE BLVT")
    @Length(max = 16)
    private String type = "SPOT";
}
