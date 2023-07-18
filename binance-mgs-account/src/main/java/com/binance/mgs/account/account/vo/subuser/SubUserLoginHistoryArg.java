package com.binance.mgs.account.account.vo.subuser;

import lombok.Data;
import org.hibernate.validator.constraints.NotEmpty;

import javax.validation.constraints.NotNull;

/**
 * Created by Fei.Huang on 2018/11/7.
 */
@Data
public class SubUserLoginHistoryArg {
    @NotNull
    private Integer page;
    @NotNull
    private Integer rows;
    private String subUserId;
    @NotEmpty
    private Long startTime;
    @NotEmpty
    private Long endTime;
}
