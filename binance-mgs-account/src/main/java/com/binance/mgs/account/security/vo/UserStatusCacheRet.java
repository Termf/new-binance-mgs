package com.binance.mgs.account.security.vo;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author Men Huatao (alex.men@binance.com)
 * @date 2021/12/6
 */
@Data
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class UserStatusCacheRet {
    private String userIdStr;
    private Boolean isDisableLogin;
}
