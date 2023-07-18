package com.binance.mgs.account.kafka;

import com.alibaba.fastjson.JSON;
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
public class CrmKafkaSupport {

    public static final String BNC_APP_CHANNEL = "BNC-App-Channel";
    private static final String REGISTER_COMPLETE = "REGISTER_COMPLETE";
	
    @Value("${crm.mgs.kafka.producer.channel.register.topic:user_event_sign_up}")
    private String crmMgsKafkaProducerChannelRegisterTopic;


    @Value("${crm.mgs.kafka.producer.channel.register.topic:user_event_sign_up}")
    private String initTopics;

    @Resource(name = "crmMgsKafkaAdmin")
    private KafkaAdmin crmMgsKafkaAdmin;

    @Resource(name = "crmMgsKafkaTemplate")
    private KafkaTemplate crmMgsKafkaTemplate;

    
    private void checkExistTopic(List<String> needCreateTopicNameList) {
        AdminClient adminClient = null;
        try {
            adminClient = AdminClient.create(crmMgsKafkaAdmin.getConfig());
            ListTopicsOptions listTopicsOptions = new ListTopicsOptions();
            listTopicsOptions.listInternal(true);
            ListTopicsResult res = adminClient.listTopics(listTopicsOptions);
            Set<String> existTopicNameSet = res.names().get();//已经创建好的topicname列表
            log.info("crm,existTopicNameSet {}",JSON.toJSONString(existTopicNameSet));
            for (String topicName : needCreateTopicNameList) {
                try {
                    boolean existFlag = existTopicNameSet.contains(topicName);
                    log.info("crm,checkExistTopic {} is {}",topicName,existFlag);
                    if (!existFlag) {
                        NewTopic newTopic = new NewTopic(topicName, 32, (short) 2);
                        List<NewTopic> topicList = Arrays.asList(newTopic);
                        adminClient.createTopics(topicList);
                        log.info("crm,topic:{}:成功创建!", topicName);
                    }
                } catch (Throwable e){
                    log.error(new FormattedMessage("crm,checkExistTopic error {}",topicName),e);
                }
            }
        } catch (Exception e) {
            log.error("crm,checkExistTopic error",e);
        } finally {
            if (null != adminClient){
                adminClient.close();
            }
        }
    }
    
    @PostConstruct
    protected void init() {
    	log.info("crm正在检测topic是否存在 开始");
        List<String> topicNameList = Lists.newArrayList(initTopics.split(","));
        this.checkExistTopic(topicNameList);
    	log.info("crm正在检测topic是否存在 结束");
    }

    public void sendCrmMgsKafkaProducerChannelRegisterTopic(Long userId,String ip,String referalId,String bncUUid,String lang) {
        Map<String,Object> param = Maps.newHashMap();
        param.put("eventType",REGISTER_COMPLETE);
        param.put("userId",userId);
        param.put("ip",ip);
        param.put("channelId",referalId);
        param.put("eventTime",String.valueOf(System.currentTimeMillis()));
        param.put("bncUUid", bncUUid);
        param.put("lang", lang);
        param.put("sourceType", "accountMgs");

        try {
            if (crmMgsKafkaTemplate != null && org.apache.commons.lang3.StringUtils.isNotBlank(crmMgsKafkaProducerChannelRegisterTopic)) {
                ListenableFuture<SendResult> sendResultFuture = crmMgsKafkaTemplate.send(crmMgsKafkaProducerChannelRegisterTopic, JSON.toJSONString(param));
                sendResultFuture.addCallback(new ListenableFutureCallback<SendResult>() {
                    @Override
                    public void onSuccess(SendResult result) {
                        log.info("sendCrmMgsKafkaProducerChannelRegisterTopic() send result success, send payload={}", JSON.toJSONString(param));
                    }
                    @Override
                    public void onFailure(Throwable ex) {
                        log.warn(String.format("sendCrmMgsKafkaProducerChannelRegisterTopic() send result failed, send payload=%s", JSON.toJSONString(param)), ex);
                    }
                });
            } else {
                log.info("sendCrmMgsKafkaProducerChannelRegisterTopic.kafka not set.");
            }
        } catch (Exception e) {
            log.error("sendCrmMgsKafkaProducerChannelRegisterTopic() error", e);
        }
    }



}
