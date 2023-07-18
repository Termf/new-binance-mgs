package com.binance.mgs.nft.cmc;

import lombok.Builder;
import lombok.Data;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import java.io.Serializable;
import java.util.Date;

@Data
@Builder
public class CmcTrancsationByDateVo implements Serializable {
    private long slug;

    @Min(value = 0, message = "Start param cannot be less than 0")
    private int start;

    @Min(value = 0, message = "Limit param cannot be less than 0")
    @Max(value = 100, message = "Limit param cannot be greater than 100")
    private int limit;

    private Date startDate;

    private Date endDate;
}
