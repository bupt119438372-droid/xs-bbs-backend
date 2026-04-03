package com.xs.bbs.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;

@ConfigurationProperties(prefix = "app.web")
public class WebProperties {

    private List<String> allowedOrigins = new ArrayList<>(List.of(
            "http://127.0.0.1:5173",
            "http://localhost:5173"
    ));
    /**
     * 本地开发时，前端可能通过局域网 IP 从手机访问，这里额外支持常见内网网段的 origin pattern。
     */
    private List<String> allowedOriginPatterns = new ArrayList<>(List.of(
            "http://127.0.0.1:*",
            "http://localhost:*",
            "http://192.168.*.*:*",
            "http://10.*.*.*:*",
            "http://172.*.*.*:*"
    ));

    public List<String> getAllowedOrigins() {
        return allowedOrigins;
    }

    public void setAllowedOrigins(List<String> allowedOrigins) {
        this.allowedOrigins = allowedOrigins;
    }

    public List<String> getAllowedOriginPatterns() {
        return allowedOriginPatterns;
    }

    public void setAllowedOriginPatterns(List<String> allowedOriginPatterns) {
        this.allowedOriginPatterns = allowedOriginPatterns;
    }
}
