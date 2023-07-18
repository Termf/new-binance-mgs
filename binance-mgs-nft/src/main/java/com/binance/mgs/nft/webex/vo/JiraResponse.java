package com.binance.mgs.nft.webex.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class JiraResponse implements Serializable {

    private Integer code;

    private JiraDto msg;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class JiraDto implements Serializable {
        private String id;

        private String key;

        private String slef;
    }
}
