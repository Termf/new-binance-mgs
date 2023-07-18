package com.binance.mgs.account.account.vo;

import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * UserSecurityLog 用户安全日志
 *
 */
@Data
public class UserSecurityLogRet implements Serializable {

    /**
     * 
     */
    private static final long serialVersionUID = 4773745784549997907L;
    private String ip; // 用户ip
    private String ipLocation; // 用户ip所在位置
    private String clientType; // 客户端类型 ios android web wap
    private String operateType; // 操作类型
    private Date operateTime; // 操作时间
    private String description; // 操作描述
}
