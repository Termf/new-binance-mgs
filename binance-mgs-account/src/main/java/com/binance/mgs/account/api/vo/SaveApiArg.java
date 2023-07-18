package com.binance.mgs.account.api.vo;

import com.binance.master.enums.AuthTypeEnum;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.hibernate.validator.constraints.Length;
import org.hibernate.validator.constraints.NotBlank;

import javax.validation.constraints.NotNull;

/**
 * Created by Fei.Huang on 2018/8/13.
 */
@Data
@EqualsAndHashCode(callSuper = false)
public class SaveApiArg extends BaseApiArg {
    /**
     * 
     */
    private static final long serialVersionUID = -7243183882885069728L;

    @NotBlank
    @Length(max = 200)
    private String apiName;

    @NotNull
    private AuthTypeEnum operationType;

    @NotBlank
    private String verifyCode;

    @Length(max = 200)
    private String info;

}

