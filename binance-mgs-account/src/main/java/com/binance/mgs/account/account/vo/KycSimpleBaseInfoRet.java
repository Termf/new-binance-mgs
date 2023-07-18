package com.binance.mgs.account.account.vo;

import lombok.Data;

import java.io.Serializable;
import java.util.Date;

@Data
public class KycSimpleBaseInfoRet implements Serializable {

    private static final long serialVersionUID = 7295807886078108219L;

    private String firstName;

    private String middleName;

    private String lastName;

    private Date dob;

    private String nationality;

}
