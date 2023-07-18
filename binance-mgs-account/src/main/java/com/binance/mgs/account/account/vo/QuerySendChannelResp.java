package com.binance.mgs.account.account.vo;

import io.swagger.annotations.ApiModel;
import lombok.Data;
import lombok.EqualsAndHashCode;



@ApiModel("用户发送渠道")
@Data
@EqualsAndHashCode(callSuper = false)
public class QuerySendChannelResp {

    private String userChannel;

}
