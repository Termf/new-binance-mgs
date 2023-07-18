package com.binance.mgs.account.api.vo;

import com.binance.mgs.account.account.vo.MultiCodeVerifyArg;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.hibernate.validator.constraints.Length;
import org.hibernate.validator.constraints.NotEmpty;

import javax.validation.constraints.NotNull;
import java.util.Set;

/**
 * Created by pcx
 */
@Data
@EqualsAndHashCode(callSuper = false)
public class SubUserUpdateApiArg extends MultiCodeVerifyArg {
    @NotNull
    @Length(max = 100)
    private String subUserEmail;

    @ApiModelProperty
    @NotNull
    private Long keyId;

    @ApiModelProperty(required = true)
    @NotNull
    private Integer ruleId;
    @ApiModelProperty(required = true)
    @NotEmpty
    private String apiName;
    @ApiModelProperty(required = false)
    @Length(min = 0)
    private String ip;
    @ApiModelProperty(required = true)
    @NotNull
    private Integer status;
    @ApiModelProperty
    private String info;
    @ApiModelProperty("symbol")
    private Set<String> symbols;

    @ApiModelProperty("第三方IP白名单配置主键")
    private Long apiManageIpConfigId;

    @ApiModelProperty("是否是前端创建")
    private Boolean isFromFE = Boolean.FALSE;
}
