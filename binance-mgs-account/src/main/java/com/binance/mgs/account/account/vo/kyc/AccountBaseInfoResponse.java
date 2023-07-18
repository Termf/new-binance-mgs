package com.binance.mgs.account.account.vo.kyc;

import java.io.Serializable;

import lombok.Data;

@Data
public class AccountBaseInfoResponse implements Serializable{

    /**
     *
     */
    private static final long serialVersionUID = 1435915530878727564L;

    private String fillType;

    private String firstName;

    private String middleName;

    private String lastName;

    private Byte gender;

    private String birthday;

    private String country;

    private String regionState;

    private String regionStateCode;

    private String city;

    private String address;

    private String postalCode;

    private String baseFillStatus;

    private String baseSubStatus;

    private String baseFillTips;

    private int kycType;

    private String taxId;

    private String nationality;

    private String companyName;

    private String companyAddress;

    private String contactNumber;

    private String registerName;

    private String registerEmail;

    private String bindMobile;

    private String mobileCode;

    private String email;

}
