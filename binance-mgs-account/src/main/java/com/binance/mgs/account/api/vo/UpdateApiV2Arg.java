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
 * Created by Fei.Huang on 2018/8/13.
 */
@Data
@EqualsAndHashCode(callSuper = false)
public class UpdateApiV2Arg extends MultiCodeVerifyArg {
    /**
     *
     */
    private static final long serialVersionUID = -4125782717694558476L;

    @NotNull
    @ApiModelProperty
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
