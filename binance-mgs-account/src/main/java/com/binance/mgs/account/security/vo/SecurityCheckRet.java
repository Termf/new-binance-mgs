package com.binance.mgs.account.security.vo;

import com.binance.account2fa.enums.AccountVerificationTwoEnum;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * @author Men Huatao (alex.men@binance.com)
 * @date 2021/12/6
 */
@Data
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class SecurityCheckRet {
    private String captchaType;
    private String sessionId;
    private String validateId;
    /**
     * 支持无密码登录
     */
    private boolean pless;
    /**
     * 无密码登录验证项
     */
    private List<AccountVerificationTwoEnum> plessVerifyTypeList;
    /**
     * loginV3的登录流程id
     */
    private String loginFlowId;
    /**
     * The goal is to replace captchaType later
     */
    private List<String> validationTypes;
    /**
     * use for attest
     */
    private String challenge;

    public SecurityCheckRet(String sessionId) {
        this.sessionId = sessionId;
    }
}
