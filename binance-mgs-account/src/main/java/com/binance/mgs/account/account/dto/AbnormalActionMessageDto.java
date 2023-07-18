package com.binance.mgs.account.account.dto;

import lombok.Data;

@Data
public class AbnormalActionMessageDto {
    /**异常行为类型参考枚举 EnumErrorLogType */
    private String type;
    /**用户id */
    private String userId;
    /**ip地址 */
    private String ipAddress;
    /**用户邮箱 */
    private String email;
    /**clientType */
    private String device;
    /**设备指纹 */
    private String detail;
}
