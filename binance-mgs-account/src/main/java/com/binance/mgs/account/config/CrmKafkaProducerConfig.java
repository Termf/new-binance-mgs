package com.binance.mgs.account.config;

import org.apache.kafka.clients.admin.AdminClientConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaAdmin;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;

import java.util.HashMap;
import java.util.Map;

@ConditionalOnProperty(value = "crm.kafka.producer.enable", havingValue = "true")
@Configuration
@EnableKafka
public class CrmKafkaProducerConfig {
    @Value("${crm.kafka.producer.bootstrapServers}")
    private String servers;
    @Value("${crm.kafka.producer.retries}")
    private int retries;
    @Value("${crm.kafka.producer.batchSize}")
    private int batchSize;
    @Value("${crm.kafka.producer.lingerMs}")
    private int linger;
    @Value("${crm.kafka.producer.bufferMemory}")
    private int bufferMemory;


    public Map<String, Object> accountProducerConfigs() {
        Map<String, Object> props = new HashMap<>();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, servers);
        props.put(ProducerConfig.RETRIES_CONFIG, retries);
        props.put(ProducerConfig.BATCH_SIZE_CONFIG, batchSize);
        props.put(ProducerConfig.LINGER_MS_CONFIG, linger);
        props.put(ProducerConfig.BUFFER_MEMORY_CONFIG, bufferMemory);
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        return props;
    }

    public ProducerFactory<String, String> accountProducerFactory() {
        return new DefaultKafkaProducerFactory<>(accountProducerConfigs());
    }

    @Bean("crmMgsKafkaTemplate")
    public KafkaTemplate<String, String> crmKafkaTemplate() {
        return new KafkaTemplate<String, String>(accountProducerFactory());
    }

    @Bean("crmMgsKafkaAdmin")
    public KafkaAdmin crmMgsKafkaAdmin() {
        Map<String, Object> configs = new HashMap<>();
        configs.put(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG,servers);
        return new KafkaAdmin(configs);
    }
}
