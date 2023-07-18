package com.binance.mgs.account.security.vo;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

/**
 * @author Men Huatao (alex.men@binance.com)
 * @date 2021/12/6
 */
@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class SecurityPreCheckRet {
    private String captchaType;
    private String sessionId;
}
