package com.binance.mgs.account.account.vo.subuser;

import lombok.Data;

import javax.validation.constraints.DecimalMax;
import javax.validation.constraints.DecimalMin;
import javax.validation.constraints.Digits;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.List;

/**
 * @author sean w
 * @date 2022/11/1
 **/
@Data
public class ManagerSubUserFeeUpdateArg {

    private List<Long> managerSubUserIds;

    @NotBlank
    private String fee;
}
