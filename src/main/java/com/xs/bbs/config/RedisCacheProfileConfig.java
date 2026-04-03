package com.xs.bbs.config;

import org.springframework.boot.autoconfigure.cache.RedisCacheManagerBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.data.redis.cache.RedisCacheConfiguration;

import java.time.Duration;

@Profile("redis")
@Configuration
public class RedisCacheProfileConfig {

    @Bean
    public RedisCacheManagerBuilderCustomizer redisCacheManagerBuilderCustomizer() {
        return builder -> builder
                .withCacheConfiguration(
                        "userProfile",
                        RedisCacheConfiguration.defaultCacheConfig().entryTtl(Duration.ofMinutes(30))
                )
                .withCacheConfiguration(
                        "userList",
                        RedisCacheConfiguration.defaultCacheConfig().entryTtl(Duration.ofMinutes(10))
                )
                .withCacheConfiguration(
                        "dailyInsight",
                        RedisCacheConfiguration.defaultCacheConfig().entryTtl(Duration.ofMinutes(15))
                )
                .withCacheConfiguration(
                        "notificationUnreadCount",
                        RedisCacheConfiguration.defaultCacheConfig().entryTtl(Duration.ofMinutes(5))
                );
    }
}
