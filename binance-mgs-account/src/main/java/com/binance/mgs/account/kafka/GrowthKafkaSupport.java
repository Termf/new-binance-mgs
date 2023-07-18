package com.binance.mgs.account.kafka;

import com.alibaba.fastjson.JSON;
import com.binance.master.utils.StringUtils;
import com.binance.mgs.account.util.GrowthRegisterAgentCodePrefixUtil;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import lombok.extern.log4j.Log4j2;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.ListTopicsOptions;
import org.apache.kafka.clients.admin.ListTopicsResult;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.logging.log4j.message.FormattedMessage;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaAdmin;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Component;
import org.springframework.util.concurrent.ListenableFuture;
import org.springframework.util.concurrent.ListenableFutureCallback;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Component
@Log4j2
public class GrowthKafkaSupport {
	
    @Value("${growth.kafka.producer.accountuserstatuschange.topic:referral-lite-register-info}")
    private String growthStatusChangeTopic;

    @Value("${growth.kafka.producer.init.topics:referral-lite-register-info}")
    private String initTopics;

    @Resource(name = "growthKafkaAdmin")
    private KafkaAdmin growthKafkaAdmin;

    @Resource(name = "growthKafkaTemplate")
    private KafkaTemplate growthKafkaTemplate;

    @Value("#{'${growth.register.agentcode.prefix:LIMIT_}'.split(',')}")
    private Set<String> growthRegisterAgentCodePrefix;
    
    private void checkExistTopic(List<String> needCreateTopicNameList) {
        AdminClient adminClient = null;
        try {
            adminClient = AdminClient.create(growthKafkaAdmin.getConfig());
            ListTopicsOptions listTopicsOptions = new ListTopicsOptions();
            listTopicsOptions.listInternal(true);
            ListTopicsResult res = adminClient.listTopics(listTopicsOptions);
            Set<String> existTopicNameSet = res.names().get();//已经创建好的topicname列表
            log.info("existTopicNameSet {}",JSON.toJSONString(existTopicNameSet));
            for (String topicName : needCreateTopicNameList) {
                try {
                    boolean existFlag = existTopicNameSet.contains(topicName);
                    log.info("checkExistTopic {} is {}",topicName,existFlag);
                    if (!existFlag) {
                        NewTopic newTopic = new NewTopic(topicName, 32, (short) 2);
                        List<NewTopic> topicList = Arrays.asList(newTopic);
                        adminClient.createTopics(topicList);
                        log.info("topic:{}:成功创建!", topicName);
                    }
                } catch (Throwable e){
                    log.error(new FormattedMessage("checkExistTopic error {}",topicName),e);
                }
            }
        } catch (Exception e) {
            log.error("checkExistTopic error",e);
        } finally {
            if (null != adminClient){
                adminClient.close();
            }
        }
    }
    
    @PostConstruct
    protected void init() {
    	log.info("mgsaccountgrowth正在检测topic是否存在 开始");
        List<String> topicNameList = Lists.newArrayList(initTopics.split(","));
        this.checkExistTopic(topicNameList);
    	log.info("mgsaccountgrowth正在检测topic是否存在 结束");
    }
    public void sendAgentToGrowthMsg(Long userId, String agentCode, Long registerTime, String devicePk, String fvideoId, String trackSource) {
        log.info("GrowthKafkaSupport.sendAgentToGrowthMsg.userId:{},agentCode:{},registerTime:{},devicePk:{},fvideoId:{},trackSource:{}",userId,agentCode,registerTime,devicePk,fvideoId,trackSource);
        /*if (userId == null || StringUtils.isBlank(agentCode) || !agentCode.startsWith(growthRegisterAgentCodePrefix)){
            return;
        }*/
        if (userId == null || StringUtils.isBlank(agentCode) || !GrowthRegisterAgentCodePrefixUtil.checkGrowthRegisterAgentCodePrefix(growthRegisterAgentCodePrefix, agentCode)){
            return;
        }
        Map<String,Object> param = Maps.newHashMap();
        param.put("userId", userId);
        param.put("agentCode", agentCode);
        param.put("registerTime", registerTime);
        param.put("devicePk", devicePk);
        param.put("fvideoId",fvideoId);
        param.put("trackSource", trackSource);
        this.sendAgentToGrowthKafka(param);
    }

    public void sendAgentToGrowthKafka(Map<String,Object> param) {
        try {
            if (growthKafkaTemplate != null && org.apache.commons.lang3.StringUtils.isNotBlank(growthStatusChangeTopic)) {
                ListenableFuture<SendResult> sendResultFuture = growthKafkaTemplate.send(growthStatusChangeTopic, JSON.toJSONString(param));
                sendResultFuture.addCallback(new ListenableFutureCallback<SendResult>() {
                    @Override
                    public void onSuccess(SendResult result) {
                        log.info("sendAgentToGrowthCMsg() send result success, send payload={}", JSON.toJSONString(param));
                    }
                    @Override
                    public void onFailure(Throwable ex) {
                        log.warn(String.format("sendAgentToGrowthCMsg() send result failed, send payload=%s", JSON.toJSONString(param)), ex);
                    }
                });
            } else {
                log.info("kafka not set.");
            }
        } catch (Exception e) {
            log.error("sendAgentToGrowthCMsg() error", e);
        }
    }
}
