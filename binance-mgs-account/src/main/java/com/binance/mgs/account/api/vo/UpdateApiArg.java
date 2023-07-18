package com.binance.mgs.account.api.vo;

import com.binance.master.enums.AuthTypeEnum;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.hibernate.validator.constraints.Length;
import org.hibernate.validator.constraints.NotEmpty;

import javax.validation.constraints.NotNull;

/**
 * Created by Fei.Huang on 2018/8/13.
 */
@Data
@EqualsAndHashCode(callSuper = false)
public class UpdateApiArg extends BaseApiArg {
    /**
     * 
     */
    private static final long serialVersionUID = -4125782717694558476L;
    @ApiModelProperty(required = true)
    @NotNull
    private Long id;
    @ApiModelProperty(required = true)
    @NotNull
    private Integer ruleId;
    @ApiModelProperty(required = true)
    @NotEmpty
    private String apiName;
    @ApiModelProperty(required = false)
    @Length(min = 0, max = 500)
    private String ip;
    @ApiModelProperty(required = true)
    @NotNull
    private Integer status;
    @ApiModelProperty(required = true)
    @NotNull
    private AuthTypeEnum operationType;
    @ApiModelProperty(required = true)
    @NotEmpty
    private String verifyCode;
    @ApiModelProperty
    private String info;
}
