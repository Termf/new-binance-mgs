package com.binance.mgs.account.account.vo.subuser;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.annotations.ApiModel;
import lombok.Data;

import java.util.Date;

@ApiModel("托管子账户划转历史返回结果")
@Data
public class ManagerSubUserTransferLogRet {

    private String id;

    private String transactionId;

    private String fromUser;

    private String fromEmail;

    private String toUser;

    private String toEmail;

    private String asset;

    private String amount;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date createTime;

    private Long createTimeStamp;

    private Long scheduledData;

    private String fromAccountType;

    private String toAccountType;

    private String status;
}
