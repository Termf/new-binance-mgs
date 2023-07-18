package com.binance.mgs.account.account.vo.subuser;

import lombok.Data;
import org.hibernate.validator.constraints.NotEmpty;

/**
 * Created by Fei.Huang on 2018/11/6.
 */
@Data
public class CreateSubUserArg {
    @NotEmpty
    private String email;
    @NotEmpty
    private String password;
}
