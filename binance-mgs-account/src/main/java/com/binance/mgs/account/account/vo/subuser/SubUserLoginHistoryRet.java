package com.binance.mgs.account.account.vo.subuser;

import lombok.Data;

/**
 * Created by Fei.Huang on 2018/11/7.
 */
@Data
public class SubUserLoginHistoryRet {
    private String clientType;
    private String description;
    private String id;
    private String ip;
    private String ipLocation;
    private String operateTime;
    private String operateType;
    private String userId;
    private String email;
}