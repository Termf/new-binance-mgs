package com.binance.mgs.account.marketing.vo;

import io.swagger.annotations.ApiModel;
import lombok.Data;

import java.io.Serializable;

@Data
@ApiModel("用户引导页返回数据")
public class GetUserFirstPageRet implements Serializable {

    private Integer guidePage;

}
