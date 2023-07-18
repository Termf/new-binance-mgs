package com.binance.mgs.account.fido.vo;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.util.List;

/**
 * Description:
 *
 * @author alven
 * @since 2022/12/28
 */
@Data
public class UserCredentialSimplifyVo {
    @ApiModelProperty("replying party id, must be the same with origin or the parent domain name of the origin")
    private String rpId;
    @ApiModelProperty("credential id")
    private String credentialId;
    @ApiModelProperty("clientId")
    private String clientId;
    @ApiModelProperty("fido证书transport类型，internal or external")
    private String transportType;
    @ApiModelProperty("transType")
    private List<String> transTypes;
    @ApiModelProperty("clientType")
    private String clientType;
    
    @ApiModelProperty("authticator indicator(8bit currently)")
    private String authInd;
}
