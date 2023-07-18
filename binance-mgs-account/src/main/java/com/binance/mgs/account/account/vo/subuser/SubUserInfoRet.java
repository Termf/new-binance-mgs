package com.binance.mgs.account.account.vo.subuser;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.util.Date;

/**
 * Created by Fei.Huang on 2018/11/7.
 */
@Data
public class SubUserInfoRet {
    private String subUserId;
    private String email;
    private Date insertTime;
    private Boolean isSubUserEnabled;
    private Boolean isUserActive;
    private Boolean isUserGoogle;
    private Boolean isMarginEnabled;
    private Boolean isFutureEnabled;
    private Boolean isAssetSubUser;
    private Boolean isAssetSubUserEnabled;
    private Boolean isNoEmailSubUser;
    private Boolean isSignedLVTRiskAgreement;
    private String mobile;
    private String remark;
    @ApiModelProperty("是否是托管账户")
    private Boolean isManagerSubUser;
    @ApiModelProperty("flexLine信用额度子账号")
    private Boolean isFlexLineCreditUser = false;
    @ApiModelProperty("flexLine交易子账号")
    private Boolean isFlexLineTradingUser = false;

}