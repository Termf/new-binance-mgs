package com.binance.mgs.account.account.dto;

import com.binance.master.commons.ToString;
import lombok.Data;

import java.util.List;

@Data
public class KycGrayConfigDto extends ToString {

    private static final long serialVersionUID = 1041992464709558303L;

    /**
     * 灰度类型
     */
    private String model;

    /**
     * 白名单用户信息
     */
    private List<Long> whiteUsers;

    private Integer grayValue;

}
