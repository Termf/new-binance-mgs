package com.binance.mgs.nft.webex.service;

import com.binance.master.utils.JsonUtils;
import com.binance.master.utils.StringUtils;
import com.binance.mgs.nft.webex.WebexConfig;
import com.binance.mgs.nft.webex.request.*;
import com.binance.mgs.nft.webex.vo.JiraPersonListVo;
import com.binance.mgs.nft.webex.vo.JiraPersonVo;
import com.binance.mgs.nft.webex.vo.JiraResponse;
import com.google.common.collect.Lists;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.ListUtils;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@AllArgsConstructor
@Slf4j
public class WebexService {

    private final WebexConfig webexConfig;

    private final RestTemplate restTemplate;

    public void postWebex(WebexRequest request) {

        if(checkPass(request)) {
            return;
        }
        WebMessage message = getWebexMessage(request.getData().getId());
        JiraTicket ticket = createJiraTicket(message);
        pushWebexMessage(ticket,message);

    }

    private boolean checkPass(WebexRequest request) {
        return webexConfig.getSwitchWebex() == 0;
    }

    private void pushWebexMessage(JiraTicket ticket, WebMessage message) {
        CreateWebMessage build = CreateWebMessage.builder().build();
        build.setToPersonEmail(message.getPersonEmail());
        build.setText(GeneratorText(ticket));
        build.setPersonId(webexConfig.getBotPersonId());
        WebMessageResponse response = postFromAPI(webexConfig.getCreateMessageUrl(), build, WebMessageResponse.class);
    }

    private String GeneratorText(JiraTicket ticket) {
       return webexConfig.getRemarkMessageTemplate().replaceFirst("\\{email\\}",ticket.getEmail())
                .replaceFirst("\\{title\\}",ticket.getTitle()).replaceFirst("\\{url\\}",ticket.getUrl());
    }

    private JiraTicket createJiraTicket(WebMessage message) {
       JiraCreateDataVo data =  createJiraCreateDataVo(message);
        JiraResponse response =  postFromAPI(webexConfig.getCreateJiraTicketProxyUrl(),data, JiraResponse.class);
        return createJiraTicket(response,message.getPersonEmail());
    }

    private JiraTicket createJiraTicket(JiraResponse response, String personEmail) {
       return JiraTicket.builder().title(response.getMsg().getKey()).email(personEmail).url(webexConfig.getCreateJiraTicketUrl().replaceFirst("\\{ticket\\}",response.getMsg().getKey())).build();
    }

    private JiraCreateDataVo createJiraCreateDataVo(WebMessage message) {
        JiraCreateFileVo build = JiraCreateFileVo.builder().project(JiraCreateFileVo.JiraField.builder().id(webexConfig.getCreateJiraProductId()).build())
                .issuetype(JiraCreateFileVo.JiraField.builder().id(webexConfig.getCreateJiraIssueType()).build()).build();
        build.setSummary(message.getHtml().replaceFirst("\\<spark-mention.*\\</spark-mention\\>", ""));
        build.setAssignee(createAssignee(message));
        build.setReporter(JiraCreateFileVo.JiraField.builder().name(message.getPersonEmail()).build());
        build.setComponents(Lists.newArrayList(JiraCreateFileVo.JiraField.builder().id(webexConfig.getCreateJiraComponentId()).build()));
        JiraCreateDataVo post = JiraCreateDataVo.builder().method("POST").url(webexConfig.getCreateJiraAuthUrl()).data(
                JiraCreateDataVo.JiraCreateDetail.builder().fields(build)
                        .build()
        ).build();
        return post;
    }


    private JiraCreateFileVo.JiraField createAssignee(WebMessage message) {
        JiraCreateFileVo.JiraField build = JiraCreateFileVo.JiraField.builder().name(message.getPersonEmail()).build();

        if(CollectionUtils.isEmpty(message.getMentionedPeople())) {
            return build;
        }
        List<JiraPersonVo> webexPersons = getWebexPerson(org.apache.commons.lang3.StringUtils.join(message.getMentionedPeople(), ","));

        if(CollectionUtils.isNotEmpty(webexPersons)) {
            List<JiraPersonVo> result = webexPersons.stream().filter(item -> !item.getType().equalsIgnoreCase("bot")).collect(Collectors.toList());
            if(CollectionUtils.isEmpty(result)) {
                return build;
            }
            build.setName(result.get(0).getEmails().get(0));
        }
        return build;
    }

    private WebMessage getWebexMessage(String id) {
        return getFromAPI(webexConfig.getGetmessageUrl().replaceFirst("\\{id\\}",id),WebMessage.class);
    }

    private List<JiraPersonVo> getWebexPerson(String id) {
        return getFromAPI(webexConfig.getGetPersonUrl().concat("?id=").concat(id), JiraPersonListVo.class).getItems();
    }

    /**
     *
     * @param responseType
     * @return
     */
    public <T> T getFromAPI(String url, Class<T> responseType)  {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.add(HttpHeaders.AUTHORIZATION, webexConfig.getWebexToken());
            HttpEntity<String> entity = new HttpEntity<String>(headers);
            ResponseEntity<String> trans = restTemplate.exchange(url,
                    HttpMethod.GET, entity, new ParameterizedTypeReference<String>() {});
            return JsonUtils.toObj(trans.getBody(),responseType);
        } catch (Exception e) {
            log.error("newSys exception:", e);
            return null;
        }
    }

    /**
     *
     * @param responseType
     * @return
     */
    public <T> List<T> getListFromAPI(String url, Class<T> responseType)  {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.add(HttpHeaders.AUTHORIZATION, webexConfig.getWebexToken());
            HttpEntity<String> entity = new HttpEntity<String>(headers);
            ResponseEntity<String> trans = restTemplate.exchange(url,
                    HttpMethod.GET, entity, new ParameterizedTypeReference<String>() {});
            return JsonUtils.toObjList(trans.getBody(),responseType);
        } catch (Exception e) {
            log.error("newSys exception:", e);
            return null;
        }
    }


    /**
     *
     * @param responseType
     * @return
     */
    public <T> T postFromAPI(String url, Object request,Class<T> responseType) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.add(HttpHeaders.AUTHORIZATION, webexConfig.getWebexToken());
            headers.add("token",webexConfig.getJiraDirectToken());
            headers.add("Content-Type","application/json");
             HttpEntity<String> requestEntity = new HttpEntity<>(JsonUtils.toJsonNotNullKey(request), headers);
            String response = restTemplate.postForObject(url, requestEntity,String.class);
            return JsonUtils.toObj(response,responseType);
        } catch (Exception e) {
            log.error("newSys exception:", e);
            throw e;
        }
    }
}
