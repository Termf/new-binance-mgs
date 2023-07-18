package com.binance.mgs.account.account.vo.google;

import lombok.Data;

/**
 * @author Men Huatao (alex.men@binance.com)
 * @date 4/25/22
 */
@Data
public class DecodedAppCheckToken {
    /**
     * The issuer identifier for the issuer of the response.
     * This value is a URL with the format
     * `https://firebaseappcheck.googleapis.com/<PROJECT_NUMBER>`, where `<PROJECT_NUMBER>` is the
     * same project number specified in the {@link DecodedAppCheckToken.aud | aud} property.
     */
    String iss;

    /**
     * The Firebase App ID corresponding to the app the token belonged to.
     * As a convenience, this value is copied over to the {@link DecodedAppCheckToken.appId | app_id} property.
     */
    String sub;

    /**
     * The audience for which this token is intended.
     * This value is a JSON array of two strings, the first is the project number of your
     * Firebase project, and the second is the project ID of the same project.
     */
    String[] aud;

    /**
     * The App Check token's expiration time, in seconds since the Unix epoch. That is, the
     * time at which this App Check token expires and should no longer be considered valid.
     */
    Number exp;

    /**
     * The App Check token's issued-at time, in seconds since the Unix epoch. That is, the
     * time at which this App Check token was issued and should start to be considered
     * valid.
     */
    Number iat;

    /**
     * The App ID corresponding to the App the App Check token belonged to.
     * This value is not actually one of the JWT token claims. It is added as a
     * convenience, and is set as the value of the {@link DecodedAppCheckToken.sub | sub} property.
     */
    String appId;
}
