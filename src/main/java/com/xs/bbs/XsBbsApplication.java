package com.xs.bbs;

import com.xs.bbs.config.AiProperties;
import com.xs.bbs.config.MatchProperties;
import com.xs.bbs.config.NotificationProperties;
import com.xs.bbs.config.OutboxProperties;
import com.xs.bbs.config.WebProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
@EnableConfigurationProperties({
        MatchProperties.class,
        OutboxProperties.class,
        NotificationProperties.class,
        WebProperties.class,
        AiProperties.class
})
public class XsBbsApplication {

    public static void main(String[] args) {
        SpringApplication.run(XsBbsApplication.class, args);
    }
}
