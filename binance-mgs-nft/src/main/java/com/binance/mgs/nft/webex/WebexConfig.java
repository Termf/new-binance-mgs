package com.binance.mgs.nft.webex;


import lombok.Data;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Configuration
@Data
public class WebexConfig {
    @Value("${nft.webex.message.switch.status:1}")
    private Integer switchWebex;

    @Value("${nft.webex.message.query.url:https://webexapis.com/v1/messages/{id}}")
    private String getmessageUrl;
    @Value("${nft.webex.message.query.url:https://webexapis.com/v1/people}")
    private String getPersonUrl;
    @Value("${nft.webex.message.create.url:https://webexapis.com/v1/messages}")
    private String createMessageUrl;
    @Value("${nft.webex.message.create.url:https://jira.toolsfdg.net/browse/{ticket}}")
    private String createJiraTicketUrl;
    @Value("${nft.webex.message.create.proxy.url:http://jira-api.k8s.qa1fdg.net/jira}")
    private String createJiraTicketProxyUrl;
    @Value("${nft.webex.message.create.proxy.url:/rest/api/2/issue}")
    private String createJiraAuthUrl;
    @Value("${nft.webex.jira.product.id:10936}")
    private String createJiraProductId;
    @Value("${nft.webex.jira.issue.type:10004}")
    private String createJiraIssueType;
    @Value("${nft.webex.jira.component.id:11637}")
    private String createJiraComponentId;
    @Value("${nft.webex.common.token:Bearer MzhlYTA0ZTItYzY0MS00ODdlLTk4NjYtMDU2YjZmNGNkZmVlZGQ2MWQyOTgtYTNm_PF84_25ae0134-b107-42fd-8c63-f776f2300739}")
    private String webexToken;
    @Value("${nft.jira.common.token:khu93dpsg243k6z5gqc3ejhqw12kamr9}")
    private String jiraDirectToken;
    @Value("${nft.jira.message.remark.template:{email}, please click {title} {url} for adding component, epic and assignee}")
    private String remarkMessageTemplate;
    @Value("${nft.jira.common.bot.person.id:Y2lzY29zcGFyazovL3VzL1BFT1BMRS83MmMxMTBlZi03YmNhLTRjODctOWQyNi02YTVhOGFlZmQyZTg}")
    private String botPersonId;
}
