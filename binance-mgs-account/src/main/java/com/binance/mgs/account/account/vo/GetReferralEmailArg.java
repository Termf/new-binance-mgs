package com.binance.mgs.account.account.vo;

import io.swagger.annotations.ApiModel;
import lombok.Data;
import org.hibernate.validator.constraints.NotEmpty;

import javax.validation.constraints.Max;
import javax.validation.constraints.NotNull;
import java.io.Serializable;

/**
 * Created by yangyang on 2019/7/12.
 */
@Data
@ApiModel("获取被推荐者emails")
public class GetReferralEmailArg implements Serializable {
    @NotEmpty
    private String agentCode;

    @NotNull
    private Integer page;

    @NotNull
    @Max(10000)
    private Integer rows;

}
