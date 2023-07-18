package com.binance.mgs.account.account.vo.face;

import com.binance.master.commons.ToString;
import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.validator.constraints.NotEmpty;

/**
 * @author liliang1
 * @date 2019-02-15 14:40
 */
@Setter
@Getter
public class FaceEmailResendArg extends ToString {
    private static final long serialVersionUID = -8166946260083180844L;

    @ApiModelProperty(required = true, notes = "邮箱")
    @NotEmpty
    private String email;

    @ApiModelProperty(required = true, notes = "类型")
    @NotEmpty
    private String type;
}
