package com.binance.mgs.account.account.helper;

import com.ctrip.framework.apollo.ConfigService;
import io.lettuce.core.TimeoutOptions;
import io.lettuce.core.cluster.ClusterClientOptions;
import io.lettuce.core.cluster.ClusterTopologyRefreshOptions;
import io.lettuce.core.resource.DefaultClientResources;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.RedisClusterConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceClientConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;

@Slf4j
public class AccountMgsRedisHelper {

    private static String redisHost = ConfigService.getAppConfig().getProperty("account.mgs.redis.host", "");

    private static Integer redisPort = ConfigService.getAppConfig().getIntProperty("account.mgs.redis.port", 6379);

    private static Boolean configEnableFlag = ConfigService.getAppConfig().getBooleanProperty("account.mgs.redis.config.enable", false);

    private static Long commonTimeOut = ConfigService.getAppConfig().getLongProperty("spring.redis.timeout", 5000L);


    private static volatile RedisTemplate<String, Object> redisTemplate=null;


    public static RedisTemplate<String, Object> getInstance() {
        if (redisTemplate == null && configEnableFlag.booleanValue() ) {
            synchronized (AccountMgsRedisHelper.class) {
                if (redisTemplate == null) {
                    RedisClusterConfiguration clusterConfiguration = new RedisClusterConfiguration();
                    clusterConfiguration.clusterNode(redisHost, redisPort);

                    ClusterTopologyRefreshOptions topologyRefreshOptions = ClusterTopologyRefreshOptions.builder()
                            .enablePeriodicRefresh(Duration.ofSeconds(10)) //按照周期刷新拓扑
                            .enableAllAdaptiveRefreshTriggers() //根据事件刷新拓扑
                            .build();

                    ClusterClientOptions clusterClientOptions = ClusterClientOptions.builder()
                            //redis命令超时时间,超时后才会使用新的拓扑信息重新建立连接
                            .timeoutOptions(TimeoutOptions.enabled(Duration.ofSeconds(10)))
                            .topologyRefreshOptions(topologyRefreshOptions)
                            //默认就是重连的，显示定义一下
                            .autoReconnect(true)
                            //和默认一样最大重定向5次，避免极端情况无止境的重定向
                            .maxRedirects(5)
                             //取消校验集群节点的成员关系
                            .validateClusterNodeMembership(false)
                            .build();

                    LettuceClientConfiguration clientConfiguration = LettuceClientConfiguration.builder()
                            .clientResources(DefaultClientResources.create())
                            .clientOptions(clusterClientOptions)
                            .commandTimeout(Duration.ofMillis(commonTimeOut))
                            .build();
                    LettuceConnectionFactory lettuceConnectionFactory = new LettuceConnectionFactory(clusterConfiguration,clientConfiguration);




                    final RedisTemplate<String, Object> template = new RedisTemplate<>();
                    template.setConnectionFactory(lettuceConnectionFactory);
                    template.setKeySerializer(new StringRedisSerializer());
                    template.setHashValueSerializer(new Jackson2JsonRedisSerializer<>(Object.class));
                    template.setValueSerializer(new Jackson2JsonRedisSerializer<>(Object.class));
                    redisTemplate = template;
                    lettuceConnectionFactory.afterPropertiesSet();
                    redisTemplate.afterPropertiesSet();
                }
            }
        }
        return redisTemplate;
    }


}
