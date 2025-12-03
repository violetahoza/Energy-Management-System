package com.vio.load_balancing.config;

import com.vio.load_balancing.strategy.ConsistentHashingStrategy;
import com.vio.load_balancing.strategy.LoadBalancingStrategy;
import com.vio.load_balancing.strategy.RoundRobinStrategy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

@Configuration
@RequiredArgsConstructor
@Slf4j
public class LoadBalancingConfig {

    private final ConsistentHashingStrategy consistentHashingStrategy;
    private final RoundRobinStrategy roundRobinStrategy;

    @Value("${app.load-balancing.strategy:consistent-hashing}")
    private String strategyType;

    @Bean
    @Primary
    public LoadBalancingStrategy loadBalancingStrategy() {
        LoadBalancingStrategy strategy;

        switch (strategyType.toLowerCase()) {
            case "round-robin":
                strategy = roundRobinStrategy;
                log.info("Using Round Robin load balancing strategy");
                break;
            case "consistent-hashing":
            default:
                strategy = consistentHashingStrategy;
                log.info("Using Consistent Hashing load balancing strategy");
                break;
        }

        return strategy;
    }
}