package com.shixi.ecommerce.config;

import com.shixi.ecommerce.internal.InternalAuthRestTemplateInterceptor;
import java.util.List;
import org.apache.hc.client5.http.config.ConnectionConfig;
import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManager;
import org.apache.hc.core5.util.TimeValue;
import org.apache.hc.core5.util.Timeout;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

@Configuration
public class RestClientConfig {
    @Bean
    public RestTemplate restTemplate(InternalAuthRestTemplateInterceptor internalAuthRestTemplateInterceptor) {
        return buildRestTemplate(
                Timeout.ofSeconds(2), Timeout.ofSeconds(4), List.of(internalAuthRestTemplateInterceptor));
    }

    @Bean
    @Qualifier("searchRestTemplate") public RestTemplate searchRestTemplate(SearchProperties searchProperties) {
        SearchProperties.OpenSearch openSearch = searchProperties.getOpenSearch();
        return buildRestTemplate(
                Timeout.ofMilliseconds(openSearch.getConnectTimeoutMs()),
                Timeout.ofMilliseconds(openSearch.getReadTimeoutMs()),
                List.of());
    }

    private RestTemplate buildRestTemplate(
            Timeout connectTimeout, Timeout responseTimeout, List<ClientHttpRequestInterceptor> interceptors) {
        PoolingHttpClientConnectionManager cm = new PoolingHttpClientConnectionManager();
        cm.setMaxTotal(200);
        cm.setDefaultMaxPerRoute(50);
        cm.setDefaultConnectionConfig(
                ConnectionConfig.custom().setConnectTimeout(connectTimeout).build());

        RequestConfig config =
                RequestConfig.custom().setResponseTimeout(responseTimeout).build();

        CloseableHttpClient client = HttpClients.custom()
                .setConnectionManager(cm)
                .setDefaultRequestConfig(config)
                .evictIdleConnections(TimeValue.ofSeconds(30))
                .build();

        HttpComponentsClientHttpRequestFactory factory = new HttpComponentsClientHttpRequestFactory(client);
        RestTemplate restTemplate = new RestTemplate(factory);
        restTemplate.setInterceptors(interceptors);
        return restTemplate;
    }
}
