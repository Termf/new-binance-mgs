package com.binance.mgs.account.authcenter.dto;

import com.binance.platform.mgs.base.vo.CommonArg;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class LoginContextDto extends CommonArg {
    private static final long serialVersionUID = 3851369967855711820L;
    private Long userId;
    private String fvideoId;
    private Boolean newDeviceFlag;
    private String loginVerifyType;
}
