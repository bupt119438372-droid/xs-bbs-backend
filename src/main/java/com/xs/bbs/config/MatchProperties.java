package com.xs.bbs.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.match")
public class MatchProperties {

    private double thoughtThreshold = 0.42D;
    private int revealThreshold = 60;

    public double getThoughtThreshold() {
        return thoughtThreshold;
    }

    public void setThoughtThreshold(double thoughtThreshold) {
        this.thoughtThreshold = thoughtThreshold;
    }

    public int getRevealThreshold() {
        return revealThreshold;
    }

    public void setRevealThreshold(int revealThreshold) {
        this.revealThreshold = revealThreshold;
    }
}
