package ru.practicum.client;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.web.client.RestClient;

// @Configuration //
public class StatClientConfig {

    @Value("${stats-server.service-id:stats-service}")
    private String statsServiceId;

    @Bean
    public StatClient statClient(DiscoveryClient discoveryClient) {
        RetryTemplate retryTemplate = createRetryTemplate();
        RestClient restClient = RestClient.create();
        return new StatClient(discoveryClient, restClient, retryTemplate, statsServiceId);
    }

    private RetryTemplate createRetryTemplate() {
        RetryTemplate retryTemplate = new RetryTemplate();

        org.springframework.retry.backoff.FixedBackOffPolicy fixedBackOffPolicy =
                new org.springframework.retry.backoff.FixedBackOffPolicy();
        fixedBackOffPolicy.setBackOffPeriod(3000L);
        retryTemplate.setBackOffPolicy(fixedBackOffPolicy);

        org.springframework.retry.policy.MaxAttemptsRetryPolicy retryPolicy =
                new org.springframework.retry.policy.MaxAttemptsRetryPolicy();
        retryPolicy.setMaxAttempts(3);
        retryTemplate.setRetryPolicy(retryPolicy);

        return retryTemplate;
    }
}