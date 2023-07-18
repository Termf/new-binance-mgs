package com.binance.mgs.account.account.vo;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

/**
 * @author Men Huatao (alex.men@binance.com)
 * @date 2021/8/25
 */
@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class SendVerifyCodeResponse {
    @ApiModelProperty("发送频率限制，单位秒")
    private Long expireTime;
}
