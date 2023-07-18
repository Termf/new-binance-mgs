package com.binance.mgs.account.account.vo;

import com.binance.account.vo.certificate.CompanyCertificateVo;
import io.swagger.annotations.ApiModel;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;

/**
 * @author liliang1
 * @date 2019-03-05 10:22
 */
@ApiModel
@Setter
@Getter
public class CompanyAuthenticationRet implements Serializable {

    private static final long serialVersionUID = 6945124687492954532L;

    private String authorizationToken;

    private CompanyCertificateVo companyCertificate;
}
