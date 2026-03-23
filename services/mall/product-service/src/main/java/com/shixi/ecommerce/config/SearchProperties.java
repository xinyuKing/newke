package com.shixi.ecommerce.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 搜索相关配置。
 *
 * @author shixi
 * @date 2026-03-20
 */
@Component
@ConfigurationProperties(prefix = "search")
public class SearchProperties {
    private String provider = "auto";
    private boolean fallbackToDb = true;
    private long cacheTtlSeconds = 60;
    private final OpenSearch openSearch = new OpenSearch();

    public String getProvider() {
        return provider;
    }

    public void setProvider(String provider) {
        this.provider = provider;
    }

    public boolean isFallbackToDb() {
        return fallbackToDb;
    }

    public void setFallbackToDb(boolean fallbackToDb) {
        this.fallbackToDb = fallbackToDb;
    }

    public long getCacheTtlSeconds() {
        return cacheTtlSeconds;
    }

    public void setCacheTtlSeconds(long cacheTtlSeconds) {
        this.cacheTtlSeconds = cacheTtlSeconds;
    }

    public OpenSearch getOpenSearch() {
        return openSearch;
    }

    public static class OpenSearch {
        private String url;
        private String index = "products";
        private String username;
        private String password;
        private int connectTimeoutMs = 2000;
        private int readTimeoutMs = 3000;
        private boolean initializeOnStartup = true;
        private int bootstrapBatchSize = 200;

        public String getUrl() {
            return url;
        }

        public void setUrl(String url) {
            this.url = url;
        }

        public String getIndex() {
            return index;
        }

        public void setIndex(String index) {
            this.index = index;
        }

        public String getUsername() {
            return username;
        }

        public void setUsername(String username) {
            this.username = username;
        }

        public String getPassword() {
            return password;
        }

        public void setPassword(String password) {
            this.password = password;
        }

        public int getConnectTimeoutMs() {
            return connectTimeoutMs;
        }

        public void setConnectTimeoutMs(int connectTimeoutMs) {
            this.connectTimeoutMs = connectTimeoutMs;
        }

        public int getReadTimeoutMs() {
            return readTimeoutMs;
        }

        public void setReadTimeoutMs(int readTimeoutMs) {
            this.readTimeoutMs = readTimeoutMs;
        }

        public boolean isInitializeOnStartup() {
            return initializeOnStartup;
        }

        public void setInitializeOnStartup(boolean initializeOnStartup) {
            this.initializeOnStartup = initializeOnStartup;
        }

        public int getBootstrapBatchSize() {
            return bootstrapBatchSize;
        }

        public void setBootstrapBatchSize(int bootstrapBatchSize) {
            this.bootstrapBatchSize = bootstrapBatchSize;
        }
    }
}
