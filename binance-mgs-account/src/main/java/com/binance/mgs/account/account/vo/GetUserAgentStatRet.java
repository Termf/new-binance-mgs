package com.binance.mgs.account.account.vo;

import io.swagger.annotations.ApiModel;
import lombok.Data;

import java.io.Serializable;

/**
 * Created by yangyang on 2019/8/23.
 */
@Data
@ApiModel("获取返佣链接列表")
public class GetUserAgentStatRet implements Serializable{

    private Long id;
    private String agentCode;
    private String promoteUrl;
    private Integer status;
    private Integer peopleNums;
    private String referralRate;
    private String agentRate;
    private String label;
    private Integer selectShare;
}
