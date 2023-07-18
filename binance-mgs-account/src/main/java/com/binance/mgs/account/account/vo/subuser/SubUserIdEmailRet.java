package com.binance.mgs.account.account.vo.subuser;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

/**
 * Created by Fei.Huang on 2018/11/7.
 */
@Data
public class SubUserIdEmailRet {
    private String userId;
    private String email;
    private Boolean isAssetSubUser;
    private Boolean isAssetSubUserEnabled;
    private Boolean isNoEmailSubUser;
    private Boolean isFutureEnabled;
    private Boolean isMarginEnabled;
    @ApiModelProperty("是否是托管账户")
    private Boolean isManagerSubUser;
    @ApiModelProperty("flexLine信用额度子账号")
    private Boolean isFlexLineCreditUser = false;
    @ApiModelProperty("flexLine交易子账号")
    private Boolean isFlexLineTradingUser = false;
}
