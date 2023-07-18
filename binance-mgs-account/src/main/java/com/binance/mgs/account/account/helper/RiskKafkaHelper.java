package com.binance.mgs.account.account.helper;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.serializer.SerializerFeature;
import com.binance.account.api.UserApi;
import com.binance.account.vo.user.request.BaseDetailRequest;
import com.binance.account.vo.user.response.BaseDetailResponse;
import com.binance.master.models.APIResponse;
import com.binance.mgs.account.account.dto.RiskLoginInfoDto;
import com.binance.platform.mgs.base.helper.BaseHelper;
import com.binance.platform.mgs.base.helper.MQHelper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

@Component
@Slf4j
public class RiskKafkaHelper extends BaseHelper {

    @Resource
    private UserApi userApi;

    @Autowired(required = false)
    @Qualifier("riskLoginKafka")
    private KafkaTemplate<Object, Object> kafkaTemplate;

    @Value("${risk.login.topic:risk_user_login_v1}")
    private String riskLoginTopic;

    @Async("securityAsync")
    public void sendLoginInfoToRiskByDto(RiskLoginInfoDto dto) {
        kafkaTemplate.send(riskLoginTopic, JSON.toJSONString(dto, SerializerFeature.WriteMapNullValue));
    }

    @Async("securityAsync")
    public void sendLoginInfoToRiskByUserId(Long userId,String ip,String source) {
        BaseDetailRequest baseDetailRequest = new BaseDetailRequest();
        baseDetailRequest.setUserId(userId);
        APIResponse<BaseDetailResponse> apiResponse = null;
        try {
            apiResponse = userApi.baseDetail(getInstance(baseDetailRequest));
        } catch (Exception e) {
            log.warn("baseDetail error.",e);
        }
        checkResponse(apiResponse);
        BaseDetailResponse baseDetailResponse = apiResponse.getData();
        RiskLoginInfoDto infoDto = new RiskLoginInfoDto();
        infoDto.setEmail(baseDetailResponse.getEmail());
        infoDto.setIp(ip);
        infoDto.setMobileCode(baseDetailResponse.getMobileCode());
        infoDto.setMobile(baseDetailResponse.getMobile());
        infoDto.setSource(source);
        infoDto.setUserId(userId);

        sendLoginInfoToRiskByDto(infoDto);
    }
}
