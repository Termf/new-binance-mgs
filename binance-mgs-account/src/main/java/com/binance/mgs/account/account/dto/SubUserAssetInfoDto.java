package com.binance.mgs.account.account.dto;

import com.binance.platform.mgs.business.asset.vo.SubUserAssetInfoArg;
import lombok.Data;

/**
 * Created by Fei.Huang on 2018/11/8.
 */
@Data
public class SubUserAssetInfoDto {
    private String page;
    private String limit;
    private String email;
    private String isSubUserEnabled;

    public SubUserAssetInfoDto(SubUserAssetInfoArg arg) {
        this.page = arg.getPage().toString();
        this.limit = arg.getRows().toString();
        this.email = arg.getEmail();
        this.isSubUserEnabled = arg.getIsSubUserEnabled();
    }
}
