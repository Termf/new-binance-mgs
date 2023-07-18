package com.binance.mgs.account.account.dto;

import com.binance.mgs.account.account.vo.subuser.GetSubUserInfoArg;
import lombok.Data;

/**
 * Created by Fei.Huang on 2018/11/8.
 */
@Data
public class GetSubUserInfoDto {
    private String page;
    private String limit;
    private String email;
    private String isSubUserEnabled;

    public GetSubUserInfoDto(GetSubUserInfoArg arg) {
        this.page = arg.getPage().toString();
        this.limit = arg.getRows().toString();
        this.email = arg.getEmail();
        this.isSubUserEnabled = arg.getIsSubUserEnabled();
    }
}
