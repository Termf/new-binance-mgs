package com.binance.mgs.account.account.vo.subuser;

import lombok.Data;
import org.hibernate.validator.constraints.NotEmpty;

/**
 * Created by pcx
 */
@Data
public class UpdateSubUserRemarkArg {
    @NotEmpty
    private String subUserEmail;
    private String remark;
}