package com.binance.mgs.account.account.dto;

import lombok.Data;

@Data
public class SubAccountStatusDto {
    private boolean isSubUserFunctionEnabled;
    private boolean isSubUser;
    private boolean isSubUserEnabled;
    private boolean isAssetSubUser;

}
