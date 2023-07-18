package com.binance.mgs.account.config;

import org.apache.commons.lang3.StringUtils;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;

import java.util.HashMap;
import java.util.Map;

@Configuration
@EnableKafka
public class RiskKafkaConfig {

    @Value("${risk.kafka.producer.bootstrapServers:kafka1.devfdg.net:9092,kafka2.devfdg.net:9092,kafka3.devfdg.net:9092}")
    private String producerBootstrapServers; //生产者连接Server地址

    @Value("${risk.kafka.producer.retries:1}")
    private String producerRetries; //生产者重试次数

    @Value("${risk.kafka.producer.batchSize:16384}")
    private String producerBatchSize;

    @Value("${risk.kafka.producer.lingerMs:1}")
    private String producerLingerMs;

    @Value("${risk.kafka.producer.bufferMemory:33554432}")
    private String producerBufferMemory;

    @Value("${risk.kafka.producer.maxBlockMsConfig:1000}")
    private String maxBlockMs;


    //法币不需要配置
    @Bean
    @ConditionalOnProperty(prefix = "risk.kafka", name = "enable")
    public ProducerFactory<Object, Object> riskKafkaProducerFactory(){
        if (StringUtils.isBlank(producerBootstrapServers)) {
            return null;
        }
        Map<String, Object> configs = new HashMap<>(); //参数
        configs.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, producerBootstrapServers);
        configs.put(ProducerConfig.RETRIES_CONFIG, producerRetries);
        configs.put(ProducerConfig.BATCH_SIZE_CONFIG, producerBatchSize);
        configs.put(ProducerConfig.LINGER_MS_CONFIG, producerLingerMs);
        configs.put(ProducerConfig.BUFFER_MEMORY_CONFIG, producerBufferMemory);
        configs.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        configs.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        configs.put(ProducerConfig.MAX_BLOCK_MS_CONFIG, maxBlockMs);
        return new DefaultKafkaProducerFactory<>(configs);
    }

    @Bean("riskLoginKafka")
    @ConditionalOnProperty(prefix = "risk.kafka", name = "enable")
    public KafkaTemplate<Object, Object> kafkaTemplate(ProducerFactory<Object, Object> riskKafkaProducerFactory) {
        return new KafkaTemplate<>(riskKafkaProducerFactory, true);
    }

}
