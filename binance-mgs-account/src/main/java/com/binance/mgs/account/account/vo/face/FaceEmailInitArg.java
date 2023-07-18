package com.binance.mgs.account.account.vo.face;

import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.validator.constraints.NotEmpty;

import java.io.Serializable;

/**
 * @author liliang1
 * @date 2018-12-07 13:45
 */
@Setter
@Getter
public class FaceEmailInitArg implements Serializable {

    private static final long serialVersionUID = -4630661949049045008L;

    @ApiModelProperty(required = true, notes = "业务标识")
    @NotEmpty
    private String id;

    @ApiModelProperty(required = true, notes = "类型")
    @NotEmpty
    private FaceTransType type;

    @ApiModelProperty(required = false, notes = "是否存在回答问题")
    private Integer haveQuestion;

}
