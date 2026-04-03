package com.xs.bbs.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.ai")
public class AiProperties {

    private String mode = "compatible";
    private String providerName = "zhipu";
    private String promptVersion = "v2";
    private int requestTimeoutMs = 10000;
    private int maxTags = 5;
    private String chatBaseUrl = "";
    private String chatPath = "/chat/completions";
    private String chatApiKey = "";
    private String chatModel = "glm-4.7";
    private String embeddingBaseUrl = "";
    private String embeddingPath = "/embeddings";
    private String embeddingApiKey = "";
    private String embeddingModel = "embedding-3";

    public String getMode() {
        return mode;
    }

    public void setMode(String mode) {
        this.mode = mode;
    }

    public String getProviderName() {
        return providerName;
    }

    public void setProviderName(String providerName) {
        this.providerName = providerName;
    }

    public String getPromptVersion() {
        return promptVersion;
    }

    public void setPromptVersion(String promptVersion) {
        this.promptVersion = promptVersion;
    }

    public int getRequestTimeoutMs() {
        return requestTimeoutMs;
    }

    public void setRequestTimeoutMs(int requestTimeoutMs) {
        this.requestTimeoutMs = requestTimeoutMs;
    }

    public int getMaxTags() {
        return maxTags;
    }

    public void setMaxTags(int maxTags) {
        this.maxTags = maxTags;
    }

    public String getChatBaseUrl() {
        return chatBaseUrl;
    }

    public void setChatBaseUrl(String chatBaseUrl) {
        this.chatBaseUrl = chatBaseUrl;
    }

    public String getChatPath() {
        return chatPath;
    }

    public void setChatPath(String chatPath) {
        this.chatPath = chatPath;
    }

    public String getChatApiKey() {
        return chatApiKey;
    }

    public void setChatApiKey(String chatApiKey) {
        this.chatApiKey = chatApiKey;
    }

    public String getChatModel() {
        return chatModel;
    }

    public void setChatModel(String chatModel) {
        this.chatModel = chatModel;
    }

    public String getEmbeddingBaseUrl() {
        return embeddingBaseUrl;
    }

    public void setEmbeddingBaseUrl(String embeddingBaseUrl) {
        this.embeddingBaseUrl = embeddingBaseUrl;
    }

    public String getEmbeddingPath() {
        return embeddingPath;
    }

    public void setEmbeddingPath(String embeddingPath) {
        this.embeddingPath = embeddingPath;
    }

    public String getEmbeddingApiKey() {
        return embeddingApiKey;
    }

    public void setEmbeddingApiKey(String embeddingApiKey) {
        this.embeddingApiKey = embeddingApiKey;
    }

    public String getEmbeddingModel() {
        return embeddingModel;
    }

    public void setEmbeddingModel(String embeddingModel) {
        this.embeddingModel = embeddingModel;
    }
}
