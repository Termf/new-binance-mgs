package com.binance.mgs.account.account.dto;

import com.binance.mgs.account.account.vo.subuser.SubUserLoginHistoryArg;
import lombok.Data;

/**
 * Created by Fei.Huang on 2018/11/8.
 */
@Data
public class SubUserLoginHistoryDto {
    private String page;
    private String limit;
    private String subUserId;
    private Long startTime;
    private Long endTime;

    public SubUserLoginHistoryDto(SubUserLoginHistoryArg arg) {
        this.page = arg.getPage().toString();
        this.limit = arg.getRows().toString();
        this.subUserId = arg.getSubUserId();
        this.startTime = arg.getStartTime();
        this.endTime = arg.getEndTime();
    }
}
