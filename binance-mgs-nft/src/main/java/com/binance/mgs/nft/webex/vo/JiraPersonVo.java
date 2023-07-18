package com.binance.mgs.nft.webex.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class JiraPersonVo implements Serializable {

    private String id;

    private List<String> emails;

    private String nickName;

    private String firstName;

    private String lastName;

    private String orgId;

    private String type;
}
