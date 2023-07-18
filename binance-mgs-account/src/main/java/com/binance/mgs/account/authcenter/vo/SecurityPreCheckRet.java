package com.binance.mgs.account.authcenter.vo;

import com.binance.account2fa.enums.AccountVerificationTwoEnum;
import lombok.Data;

import java.util.List;

/**
 * @author Men Huatao (alex.men@binance.com)
 * @date 2021/12/6
 */
@Data
public class SecurityPreCheckRet {
    private boolean reject;
    private boolean needCheck;
    private String captchaType;
    private String validateId;
    private Boolean isPless = false;
    /**
     * 无密码登录验证项
     */
    private List<AccountVerificationTwoEnum> plessVerifyTypeList;

    private String rejectMsg;
    private String loginFlowId;
    private List<String> validationTypes;
    private String challenge;
}
