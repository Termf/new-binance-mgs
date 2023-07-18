package com.binance.mgs.account.account.vo.new2fa;

import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * @author rudy.c
 * @date 2023-05-02 21:09
 */
@Data
public class GetUserVerificationBindRet implements Serializable {
    private static final long serialVersionUID = 1L;
    private Boolean isBindEmail;
    private String email;
    private Date bindEmailTime;

    private Boolean isBindMobile;
    private String mobileCode;
    private String mobile;
    private Date bindMobileTime;
    
    private Boolean isBindGoogle;
    private Date bindGoogleTime;
}
