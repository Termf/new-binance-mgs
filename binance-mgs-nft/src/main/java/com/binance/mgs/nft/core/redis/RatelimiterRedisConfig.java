package com.binance.mgs.nft.core.redis;

import lombok.Data;
import org.springframework.boot.autoconfigure.data.redis.RedisUseJedisAutoConfiguration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisClusterConfiguration;
import org.springframework.data.redis.connection.jedis.JedisClientConfiguration;
import org.springframework.data.redis.connection.jedis.JedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import redis.clients.jedis.JedisPoolConfig;

import java.time.Duration;
import java.util.Arrays;

@EnableCaching
@Configuration
public class RatelimiterRedisConfig {

    @Bean(name = "rateLimiterRedisTemplate")
    RedisTemplate<String, Object> rateLimiterRedisTemplate(InfraRedisProperties redisProperties) {
        JedisConnectionFactory connectionFactory = createJedisConnectionFactory(redisProperties);
        connectionFactory.afterPropertiesSet();
        RedisSerializer<Object> serializer = new RedisUseJedisAutoConfiguration.CustomizeGenericFastJsonRedisSerializer();
        StringRedisSerializer stringRedisSerializer = new StringRedisSerializer();
        RedisTemplate<String, Object> template = new RedisTemplate();
        template.setConnectionFactory(connectionFactory);
        template.setValueSerializer(serializer);
        template.setHashValueSerializer(serializer);
        template.setKeySerializer(stringRedisSerializer);
        template.setDefaultSerializer(serializer);
        template.afterPropertiesSet();
        return template;

    }

    private JedisConnectionFactory createJedisConnectionFactory(InfraRedisProperties redisProperties) {
        JedisClientConfiguration clientConfiguration = getJedisClientConfiguration(redisProperties);
        RedisClusterConfiguration clusterConfiguration = new RedisClusterConfiguration(Arrays.asList(redisProperties.getClusterNodes()));
        return new JedisConnectionFactory(clusterConfiguration, clientConfiguration);
    }

    private JedisClientConfiguration getJedisClientConfiguration(InfraRedisProperties redisProperties) {
        JedisClientConfiguration.JedisClientConfigurationBuilder builder = JedisClientConfiguration.builder();
        builder.usePooling().poolConfig(jedisPoolConfig(redisProperties));
        builder.readTimeout(Duration.ofMillis(redisProperties.getReadTimeoutMs()));
        return builder.build();
    }

    private JedisPoolConfig jedisPoolConfig(InfraRedisProperties redisProperties) {
        JedisPoolConfig poolConfig = new JedisPoolConfig();
        poolConfig.setMaxIdle(redisProperties.getPoolMaxIdle());
        poolConfig.setMinIdle(redisProperties.getPoolMinIdle());
        poolConfig.setMaxTotal(redisProperties.getPoolMaxActive());
        poolConfig.setMaxWaitMillis(redisProperties.getMaxWaitMs());
        return poolConfig;
    }

    @Data
    @Configuration
    @ConfigurationProperties(prefix = "mgs.nft.redis.infra")
    public static class InfraRedisProperties {
        private String clusterNodes;
        private Integer poolMaxActive = 12;
        private Integer poolMaxIdle = 12;
        private Integer poolMinIdle = 1;
        // max wait time
        private Integer maxWaitMs = 2000;
        private Integer readTimeoutMs = 500;
    }
}
