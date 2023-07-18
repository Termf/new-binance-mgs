package com.binance.mgs.nft.webex.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class JiraCreateDataVo implements Serializable {

    private String method;

    private String url;

    private JiraCreateDetail data ;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class JiraCreateDetail {

        private JiraCreateFileVo fields;
    }

}
