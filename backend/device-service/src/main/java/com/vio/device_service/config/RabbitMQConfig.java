package com.vio.device_service.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    @Value("${app.rabbitmq.exchange.sync:sync.exchange}")
    private String syncExchange;

    @Value("${app.rabbitmq.queue.sync:sync.queue}")
    private String syncQueue;

    @Value("${app.rabbitmq.routing-key.user-sync:user.sync}")
    private String userSyncRoutingKey;

    @Value("${app.rabbitmq.routing-key.device-sync:device.sync}")
    private String deviceSyncRoutingKey;

    // Exchange declaration
    @Bean
    public TopicExchange syncExchange() {
        return new TopicExchange(syncExchange);
    }

    // Queue declaration
    @Bean
    public Queue syncQueue() {
        return QueueBuilder.durable(syncQueue)
                .build();
    }

    // Binding for user sync events (Device Service listens to user events)
    @Bean
    public Binding userSyncBinding(Queue syncQueue, TopicExchange syncExchange) {
        return BindingBuilder
                .bind(syncQueue)
                .to(syncExchange)
                .with(userSyncRoutingKey);
    }

    // Binding for device sync events
    @Bean
    public Binding deviceSyncBinding(Queue syncQueue, TopicExchange syncExchange) {
        return BindingBuilder
                .bind(syncQueue)
                .to(syncExchange)
                .with(deviceSyncRoutingKey);
    }

    // Message converter for JSON serialization
    @Bean
    public MessageConverter messageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    // RabbitTemplate configuration
    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate rabbitTemplate = new RabbitTemplate(connectionFactory);
        rabbitTemplate.setMessageConverter(messageConverter());
        return rabbitTemplate;
    }
}