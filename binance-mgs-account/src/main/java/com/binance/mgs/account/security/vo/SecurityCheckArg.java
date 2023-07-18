package com.binance.mgs.account.security.vo;

import com.binance.platform.mgs.business.captcha.vo.ValidateCodeArg;
import lombok.Data;

/**
 * @author Men Huatao (alex.men@binance.com)
 * @date 2021/12/6
 */
@Data
public class SecurityCheckArg extends ValidateCodeArg {
    private static final long serialVersionUID = 4370516205141168172L;
}
