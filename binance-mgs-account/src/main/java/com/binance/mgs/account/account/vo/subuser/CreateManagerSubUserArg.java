package com.binance.mgs.account.account.vo.subuser;

import com.binance.master.enums.TerminalEnum;
import com.binance.mgs.account.account.vo.MultiCodeVerifyArg;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import org.hibernate.validator.constraints.NotEmpty;

import javax.validation.constraints.NotBlank;
import java.util.HashMap;

/**
 */
@Data
public class CreateManagerSubUserArg {
    @ApiModelProperty(required = true, notes = "托管账号username")
    @NotBlank
    private String userName;

}
