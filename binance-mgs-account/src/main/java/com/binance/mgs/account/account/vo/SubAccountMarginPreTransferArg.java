package com.binance.mgs.account.account.vo;

import com.binance.platform.mgs.base.vo.CommonArg;
import io.swagger.annotations.ApiModel;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.hibernate.validator.constraints.Length;

import javax.validation.constraints.NotBlank;

@ApiModel("main检测最大划转")
@Data
@EqualsAndHashCode(callSuper = false)
public class SubAccountMarginPreTransferArg extends CommonArg {
    private static final long serialVersionUID = 2595088303074381781L;

    @NotBlank(message = "email is not null")
    private String email;

    @NotBlank
    @Length(max = 50)
    private String asset;

}
