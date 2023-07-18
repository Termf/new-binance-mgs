package com.binance.mgs.account.account.vo.subuser;

import io.swagger.annotations.ApiModel;
import lombok.Data;

/**
 * @author sean w
 * @date 2022/10/26
 **/
@ApiModel("根据交易团队查询托管子账户请求参数")
@Data
public class GetManagerSubInfoArg {
    private String isSubUserEnabled;

    private Integer page;

    private Integer limit;
}
