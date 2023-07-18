package com.binance.mgs.account.account.vo;

import com.binance.platform.mgs.base.vo.CommonArg;
import io.swagger.annotations.ApiModel;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.validator.constraints.Length;

import javax.validation.constraints.NotNull;
import java.util.Date;

@ApiModel(description = "kyc个人信息", value = "kyc个人信息")
@Getter
@Setter
public class KycBaseInfoArg extends CommonArg {

    private static final long serialVersionUID = 514610868806334220L;

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
     * 曾用名
     */
    private String formerFirstName;

    /**
     * 曾用中间名
     */
    private String formerMiddleName;

    /**
     * 曾用姓
     */
    private String formerLastName;

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
     * 地址
     */
    @Length(max = 500, message = "address size [1, 500]")
    @NotNull
    private String address;

    /**
     * 邮编
     */
    @Length(max = 50, message = "postal code size [1, 50]")
    private String postalCode;

    /**
     * 城市
     */
    @Length(min = 1, max = 128, message = "city size [1, 128]")
    @NotNull
    private String city;

    /**
     * 国家
     */
    @NotNull
    private String country;

}
