package com.xiaohua.novel.chat.application;

import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "app.ai.chat")
public class ChatModelProperties {

    private String defaultProvider = "zhipu";
    private String defaultModel = "glm-4.7-flash";
    private int timeoutSeconds = 90;
    private Map<String, ProviderProperties> providers = new LinkedHashMap<>();

    public String getDefaultProvider() {
        return defaultProvider;
    }

    public void setDefaultProvider(String defaultProvider) {
        this.defaultProvider = defaultProvider;
    }

    public String getDefaultModel() {
        return defaultModel;
    }

    public void setDefaultModel(String defaultModel) {
        this.defaultModel = defaultModel;
    }

    public int getTimeoutSeconds() {
        return timeoutSeconds;
    }

    public void setTimeoutSeconds(int timeoutSeconds) {
        this.timeoutSeconds = timeoutSeconds;
    }

    public Map<String, ProviderProperties> getProviders() {
        return providers;
    }

    public void setProviders(Map<String, ProviderProperties> providers) {
        this.providers = providers;
    }

    public static class ProviderProperties {

        private String displayName;
        private String baseUrl;
        private String apiKey;
        private Map<String, ModelProperties> models = new LinkedHashMap<>();

        public String getDisplayName() {
            return displayName;
        }

        public void setDisplayName(String displayName) {
            this.displayName = displayName;
        }

        public String getBaseUrl() {
            return baseUrl;
        }

        public void setBaseUrl(String baseUrl) {
            this.baseUrl = baseUrl;
        }

        public String getApiKey() {
            return apiKey;
        }

        public void setApiKey(String apiKey) {
            this.apiKey = apiKey;
        }

        public Map<String, ModelProperties> getModels() {
            return models;
        }

        public void setModels(Map<String, ModelProperties> models) {
            this.models = models;
        }
    }

    public static class ModelProperties {

        private String displayName;
        private boolean freeTier;
        private String description;

        public String getDisplayName() {
            return displayName;
        }

        public void setDisplayName(String displayName) {
            this.displayName = displayName;
        }

        public boolean isFreeTier() {
            return freeTier;
        }

        public void setFreeTier(boolean freeTier) {
            this.freeTier = freeTier;
        }

        public String getDescription() {
            return description;
        }

        public void setDescription(String description) {
            this.description = description;
        }
    }
}
