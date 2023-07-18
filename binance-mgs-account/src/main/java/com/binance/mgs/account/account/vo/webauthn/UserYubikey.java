package com.binance.mgs.account.account.vo.webauthn;

import com.binance.platform.mgs.serializer.LongToStringSerializer;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.util.Date;

@Setter
@Getter
public class UserYubikey implements Serializable {

    @JsonSerialize(using = LongToStringSerializer.class)
    private Long id;

    private String origin;

    private String nickName;

//   private Long signatureCount;

    private Boolean isLegacy;

    private Date createTime;

    private Date updateTime;

}
