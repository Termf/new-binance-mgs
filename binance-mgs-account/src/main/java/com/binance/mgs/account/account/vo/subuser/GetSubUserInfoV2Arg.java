package com.binance.mgs.account.account.vo.subuser;

import lombok.Data;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;

/**
 * Created by Fei.Huang on 2018/11/7.
 */
@Data
public class GetSubUserInfoV2Arg {
    @NotNull
    @Min(1)
    private Integer page;
    @NotNull
    @Min(1)
    @Max(1000)
    private Integer rows;
    private String email;
    private String isSubUserEnabled;
    private String remark;
}
