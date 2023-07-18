package com.binance.mgs.account.oauth.dto;

import lombok.Data;

/**
 * {"access_token":"d580fbfe-da2c-4840-8b66-848168ad8d62","token_type":"bearer","refresh_token":"9406e12f-d62e-42bd-ad40-0206d94ae776","expires_in":43199,"scope":"read
 * write"}
 *
 */
@Data
public class AccessTokenDto {
    private String accessToken;
    private String tokenType;
    private String refreshToken;
    private String scope;
    private int expiresIn;
}
