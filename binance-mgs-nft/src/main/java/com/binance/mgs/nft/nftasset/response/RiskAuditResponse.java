package com.binance.mgs.nft.nftasset.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@NoArgsConstructor
@Builder
@AllArgsConstructor
public class RiskAuditResponse implements Serializable {
    //风控审核，是否命中
    private boolean isHit;
    private String decisionCode;
    //等待审核结果的临时标识
    private String checkAuditId;
    //资源自动审核用户不合格次数以及用户不合格等级
    private String inappropriateLevel;
    private Integer InappropriateAttempts;

}
