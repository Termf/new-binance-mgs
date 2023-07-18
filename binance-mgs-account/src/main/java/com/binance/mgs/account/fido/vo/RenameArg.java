package com.binance.mgs.account.fido.vo;

import com.binance.platform.mgs.base.vo.CommonArg;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.hibernate.validator.constraints.Length;

import javax.validation.constraints.NotBlank;

@EqualsAndHashCode(callSuper = true)
@Data
public class RenameArg extends CommonArg {

    @ApiModelProperty("credential id")
    @NotBlank
    private String credentialId;

    @ApiModelProperty("New Name")
    @NotBlank
    @Length(max = 20)
    private String newName;
}
