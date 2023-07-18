package com.binance.mgs.account.account.vo.kyc;

import lombok.Data;

import java.io.Serializable;
import java.util.Date;

@Data
public class AccountGetKycStatusResponse implements Serializable{

	/**
	 *
	 */
	private static final long serialVersionUID = -1698011657590845598L;

	private int kycType;

    private Integer kycLevel;

    private String status;

    private Date createTime;

    private Date updateTime;

    private String messageTips;

    private String baseFillStatus;

    private String baseFillTips;

    private String addressStatus;

    private String addressTips;

    private String bindMobile;

    private String jumioStatus;

    private String jumioTips;

    private String faceStatus;

    private String faceTips;

    private String remark;

    private String mobileCode;

    private boolean kycFaceSwitch;

    private String nextStep;

    private String googleFormStatus;

    private String googleFormTips;

    private boolean needAddress;

}
