package com.binance.mgs.account.account.dto;

import com.binance.platform.mgs.enums.EnumErrorLogType;
import lombok.Data;

@Data
public class AbnormalActionDto {
    /**异常行为类型参考枚举 EnumErrorLogType */
    private EnumErrorLogType type;
    /**用户id */
    private Long userId;
    /**ip地址 */
    private String ipAddress;
    /**用户邮箱 */
    private String email;
    /**行为次数，可以不发到risk */
    private Long count;
    /**clientType */
    private String clientType;
    /**设备指纹 */
    private String device;
}
