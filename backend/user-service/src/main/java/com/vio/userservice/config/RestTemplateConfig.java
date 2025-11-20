package com.vio.userservice.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;

// Configuration class to define a RestTemplate bean for making HTTP requests to other services
@Configuration
public class RestTemplateConfig {

    @Bean
    public RestTemplate restTemplate() {
        HttpComponentsClientHttpRequestFactory factory = new HttpComponentsClientHttpRequestFactory(); // provides config for HTTP client connections
        factory.setConnectTimeout(Duration.ofSeconds(5));
        factory.setConnectionRequestTimeout(Duration.ofSeconds(5));
        RestTemplate restTemplate = new RestTemplate(factory);
        return restTemplate;
    }
}