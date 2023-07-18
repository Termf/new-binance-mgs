package com.binance.mgs.account.account.vo.webauthn;

import com.alibaba.fastjson.JSONObject;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;

@Getter
@Setter
public class StartRegisterRet implements Serializable {

    private String requestId;

    private JSONObject creationOptions;

}
