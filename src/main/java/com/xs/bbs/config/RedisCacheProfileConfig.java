package com.xs.bbs.config;

import org.springframework.boot.autoconfigure.cache.RedisCacheManagerBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;

import java.time.Duration;

@Profile("redis")
@Configuration
public class RedisCacheProfileConfig {

    @Bean
    public RedisCacheManagerBuilderCustomizer redisCacheManagerBuilderCustomizer() {
        RedisCacheConfiguration baseConfig = RedisCacheConfiguration.defaultCacheConfig()
                // 使用 JSON 序列化缓存值，避免 JDK 序列化要求业务对象实现 Serializable。
                .serializeValuesWith(
                        RedisSerializationContext.SerializationPair.fromSerializer(
                                new GenericJackson2JsonRedisSerializer()
                        )
                );
        return builder -> builder
                .withCacheConfiguration(
                        "userProfile",
                        baseConfig.entryTtl(Duration.ofMinutes(30))
                )
                .withCacheConfiguration(
                        "userList",
                        baseConfig.entryTtl(Duration.ofMinutes(10))
                )
                .withCacheConfiguration(
                        "dailyInsight",
                        baseConfig.entryTtl(Duration.ofMinutes(15))
                )
                .withCacheConfiguration(
                        "notificationUnreadCount",
                        baseConfig.entryTtl(Duration.ofMinutes(5))
                );
    }
}
