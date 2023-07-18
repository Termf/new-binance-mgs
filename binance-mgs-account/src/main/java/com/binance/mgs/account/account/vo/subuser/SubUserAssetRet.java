package com.binance.mgs.account.account.vo.subuser;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

/**
 * Created by Fei.Huang on 2018/11/7.
 */
@Data
public class SubUserAssetRet {
    private String subUserId;
    private String email;
    private Boolean isSubUserEnabled;
    private String totalAsset;
    private Boolean isAssetSubUser;
    private Boolean isAssetSubUserEnabled;
    private Boolean isManagerSubUser;
    @ApiModelProperty("flexLine信用额度子账号")
    private Boolean isFlexLineCreditUser = false;
    @ApiModelProperty("flexLine交易子账号")
    private Boolean isFlexLineTradingUser = false;
}
