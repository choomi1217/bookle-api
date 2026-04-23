package com.bookle.wordbook.config;

import java.time.Duration;
import org.springframework.boot.web.client.ClientHttpRequestFactories;
import org.springframework.boot.web.client.ClientHttpRequestFactorySettings;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
public class RestClientConfig {

    private static final String CLAUDE_API_URL = "https://api.anthropic.com";

    @Bean
    RestClient claudeRestClient() {
        ClientHttpRequestFactorySettings settings = ClientHttpRequestFactorySettings.DEFAULTS
            .withConnectTimeout(Duration.ofSeconds(3))
            .withReadTimeout(Duration.ofSeconds(30));

        return RestClient.builder()
            .baseUrl(CLAUDE_API_URL)
            .requestFactory(ClientHttpRequestFactories.get(settings))
            .build();
    }
}
