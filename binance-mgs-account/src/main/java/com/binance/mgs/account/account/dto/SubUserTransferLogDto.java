package com.binance.mgs.account.account.dto;

import com.binance.platform.mgs.business.asset.vo.SubUserTransferLogArg;
import lombok.Data;

/**
 * Created by Fei.Huang on 2018/11/8.
 */
@Data
public class SubUserTransferLogDto {
    private String page;
    private String limit;
    private String userId;
    private String transfers;
    private Long startCreateTime;
    private Long endCreateTime;

    public SubUserTransferLogDto(SubUserTransferLogArg arg) {
        this.page = arg.getPage().toString();
        this.limit = arg.getRows().toString();
        this.userId = arg.getUserId();
        this.transfers = TransfersEnum.getTransfers(arg.getTransfers());
        this.startCreateTime = arg.getStartTime();
        this.endCreateTime = arg.getEndTime();
    }

    enum TransfersEnum {
        from,
        to;

        public static String getTransfers(String value) {
            if (TransfersEnum.from.name().equalsIgnoreCase(value) || TransfersEnum.to.name().equalsIgnoreCase(value)) {
                return TransfersEnum.valueOf(value).name();
            }
            return null;
        }
    }
}
