package com.binance.mgs.account.authcenter.vo;

import com.binance.platform.mgs.base.vo.CommonArg;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.hibernate.validator.constraints.Range;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

@ApiModel
@Data
@EqualsAndHashCode(callSuper = false)
public class CreateQrCodeUrlArg extends CommonArg {
    @ApiModelProperty("二维码类型")
    @NotBlank
    private String bizType;
    @ApiModelProperty("app deepLink queryString,例如:key1=value1&key2=value2")
    private String deepLinkQueryString;
    @ApiModelProperty("跳转url queryString,例如:key1=value1&key2=value2")
    private String urlPathQueryString;
    @ApiModelProperty("1： 加速域名  2：webView 域名  3： webUrl")
    @NotNull
    @Range(min = 1,max = 3)
    private Integer domainType;
    @ApiModelProperty("1: domain 方式请求  2： webView方式请求")
    @NotNull
    @Range(min = 1,max = 2)
    private Integer requestType;
}
