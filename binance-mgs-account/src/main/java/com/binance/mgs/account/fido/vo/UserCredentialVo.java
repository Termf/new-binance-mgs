package com.binance.mgs.account.fido.vo;

import com.binance.fido2.vo.GetAllCredentialsResponse;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

/**
 * Created by Kay.Zhao on 2022/3/10
 */
@Data
public class UserCredentialVo extends GetAllCredentialsResponse {
    
    private boolean isCurrentDevice = false;

    @ApiModelProperty("fido证书transport类型，internal or external")
    private String transportType;
    
}
