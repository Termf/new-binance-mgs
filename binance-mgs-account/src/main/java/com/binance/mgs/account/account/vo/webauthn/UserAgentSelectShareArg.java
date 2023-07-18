package com.binance.mgs.account.account.vo.webauthn;

import io.swagger.annotations.ApiModel;
import lombok.Data;
import org.hibernate.validator.constraints.NotBlank;

import java.io.Serializable;

/**
 * Created by yangyang on 2019/7/12.
 */
@Data
@ApiModel("选中某个agentCode作为分享code")
public class UserAgentSelectShareArg implements Serializable {
    @NotBlank
    private String agentCode;

}
