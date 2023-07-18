package com.binance.mgs.account.account.vo.subuser;

import com.google.common.collect.Lists;
import io.swagger.annotations.ApiModel;
import lombok.Data;

import java.util.List;

@Data
@ApiModel("ClearPositionResponse")
public class ClearPositionRet {

    private List<ClearPositionFailedOrderVo> failOrderList = Lists.newArrayList();
}
