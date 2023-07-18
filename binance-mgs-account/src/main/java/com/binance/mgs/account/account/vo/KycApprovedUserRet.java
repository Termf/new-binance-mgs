package com.binance.mgs.account.account.vo;

import lombok.Data;

import java.util.Date;

/**
 * Created by Fei.Huang on 2018/8/9.
 */
@Data
public class KycApprovedUserRet {

    private Long userId;
    private String email;
    private BaseInfo baseInfo;
    private CheckInfo checkInfo;
    private Date approveTime;
    private String front;
    private String back;
    private String face;

    @Data
    public static class BaseInfo {
        String firstName;
        String middleName;
        String lastName;
        Date dob;
        String address;
        String postalCode;
        String city;
        String country;
    }

    @Data
    public static class CheckInfo {
        String type;
        String issuingCountry;
        String expiryDate;
        String firstName;
        String lastName;
        String dob;
        String number;
        String postalCode;
        String city;
        String address;
    }
}