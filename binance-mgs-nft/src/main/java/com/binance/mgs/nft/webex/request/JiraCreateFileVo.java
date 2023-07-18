package com.binance.mgs.nft.webex.request;

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
public class JiraCreateFileVo implements Serializable {

    private JiraField project;

    private String summary;

    private JiraField assignee;

    private JiraField reporter;

    private JiraField issuetype;

    private List<JiraField> components;


    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class JiraField implements Serializable {

        private String id;
        private String name;
    }

}
