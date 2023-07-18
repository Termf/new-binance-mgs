package com.binance.mgs.account.account.vo.subuser;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

import java.util.Date;

/**
 */
@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class SubUserTransferLogRetV2 {
    private String id;
    private String transactionId;
    private String fromUser;
    private String fromEmail;
    private String toUser;
    private String toEmail;
    private String asset;
    private String symbol;
    private String amount;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date createTime;
    private Long createTimeStamp;
    private String fromAccountType;
    private String toAccountType;
    private String status;


}
