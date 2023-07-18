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
public class JiraPersonListVo implements Serializable {

    private List<JiraPersonVo> items;
}
