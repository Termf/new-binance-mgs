package com.binance.mgs.account.account.vo;

import com.binance.account.common.enums.OrderConfirmType;
import com.binance.platform.mgs.base.vo.CommonArg;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.Valid;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.util.List;

@ApiModel("用户下单确认状态RequestV2")
@Data
public class OrderConfirmStatusV2Arg extends CommonArg {
    private static final long serialVersionUID = -3100988708105275305L;

    @Data
    public static class OrderConfirmItem {

        @ApiModelProperty("类型")
        @NotNull
        private OrderConfirmType orderConfirmType;

        @ApiModelProperty("true:启用 false 停用")
        private boolean status;

    }

    @NotEmpty
    @Valid
    private List<@NotNull OrderConfirmItem> orderConfirmList;

}