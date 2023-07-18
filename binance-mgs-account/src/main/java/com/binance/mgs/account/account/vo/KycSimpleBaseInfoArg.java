package com.binance.mgs.account.account.vo;

import com.binance.platform.mgs.base.vo.CommonArg;
import io.swagger.annotations.ApiModel;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.validator.constraints.Length;

import javax.validation.constraints.NotNull;
import java.util.Date;

@ApiModel(description = "ug simple kyc个人信息", value = "ug simple kyc个人信息")
@Getter
@Setter
public class KycSimpleBaseInfoArg extends CommonArg {

    private static final long serialVersionUID = 2689743215222720544L;

    /**
     * 名
     */
    @Length(max = 128, message = "first name size [1, 128]")
    @NotNull
    private String firstName;

    /**
     * 中间名
     */
    @Length(min = 0, max = 128, message = "middle name size [0, 128]")
    private String middleName;

    /**
     * 姓
     */
    @Length(max = 128, message = "last name size [1, 128]")
    @NotNull
    private String lastName;


    /**
     * 生日
     */
    @NotNull
    private Date dob;

    /**
     * 国籍
     */
    private String nationality;

    /**
     * 证件类型
     */
    private String idType;

    /**
     * 证件id
     */
    private String idno;


}
