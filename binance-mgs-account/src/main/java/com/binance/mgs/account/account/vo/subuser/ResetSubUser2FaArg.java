package com.binance.mgs.account.account.vo.subuser;

import lombok.Data;
import org.hibernate.validator.constraints.NotEmpty;

/**
 * Created by Fei.Huang on 2018/11/7.
 */
@Data
public class ResetSubUser2FaArg {
    @NotEmpty
    private String subUserId;
    @NotEmpty
    private String subType;
    @NotEmpty
    private String parentCode;
    @NotEmpty
    private String parentAuthType;
}
