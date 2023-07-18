package com.binance.mgs.account.account.vo;

import com.binance.account.vo.user.response.UserSignatureConfigResponse;
import io.swagger.annotations.ApiModel;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * @author sean w
 * @date 2022/8/3
 **/
@ApiModel("特殊国家broker用户签署条款response")
@Data
@EqualsAndHashCode(callSuper = false)
public class UserSpecialSignatureConfigResponse extends UserSignatureConfigResponse {
    private boolean isNeedSign;
}
