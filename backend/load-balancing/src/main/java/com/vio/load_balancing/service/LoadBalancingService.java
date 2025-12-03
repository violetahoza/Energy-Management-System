package com.vio.load_balancing.service;

import com.vio.load_balancing.event.DeviceDataMessage;
import com.vio.load_balancing.strategy.LoadBalancingStrategy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class LoadBalancingService {

    private final RabbitTemplate rabbitTemplate;
    private final LoadBalancingStrategy loadBalancingStrategy;

    @Value("${app.monitoring.replicas:3}")
    private int totalReplicas;

    @Value("${app.rabbitmq.exchange.ingest}")
    private String ingestExchange;

    @Value("${app.rabbitmq.routing-key.ingest-prefix}")
    private String ingestRoutingKeyPrefix;

    public void routeMessage(DeviceDataMessage message) {
        try {
            // Select replica using configured strategy
            int replicaNumber = loadBalancingStrategy.selectReplica(message, totalReplicas);

            // Build routing key for the selected replica
            String routingKey = ingestRoutingKeyPrefix + replicaNumber;

            // Send message to replica-specific ingest queue
            rabbitTemplate.convertAndSend(ingestExchange, routingKey, message);

            log.info("Routed device {} data to replica {} (queue: {})",
                    message.getDeviceId(), replicaNumber, routingKey);

        } catch (Exception e) {
            log.error("Error routing message for device {}: {}",
                    message.getDeviceId(), e.getMessage(), e);
            throw new RuntimeException("Failed to route message", e);
        }
    }
}