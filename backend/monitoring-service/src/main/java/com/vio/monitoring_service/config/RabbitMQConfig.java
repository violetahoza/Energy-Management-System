package com.vio.monitoring_service.config;

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

    @Value("${app.rabbitmq.queue.device-data}")
    private String deviceDataQueue;

    @Value("${app.rabbitmq.queue.sync}")
    private String syncQueue;

    @Value("${app.rabbitmq.exchange.device-data}")
    private String deviceDataExchange;

    @Value("${app.rabbitmq.exchange.sync}")
    private String syncExchange;

    @Value("${app.rabbitmq.routing-key.device-data}")
    private String deviceDataRoutingKey;

    @Value("${app.rabbitmq.routing-key.user-sync}")
    private String userSyncRoutingKey;

    @Value("${app.rabbitmq.routing-key.device-sync}")
    private String deviceSyncRoutingKey;

    @Bean
    public Queue deviceQueue() {
        return new Queue(deviceDataQueue, true);
    }

    @Bean
    public Queue syncQueue() {
        return new Queue(syncQueue, true);
    }

    @Bean
    public TopicExchange deviceExchange() {
        return new TopicExchange(deviceDataExchange);
    }

    @Bean
    public TopicExchange syncExchange() {
        return new TopicExchange(syncExchange);
    }

    @Bean
    public Binding deviceBinding() {
        return BindingBuilder.bind(deviceQueue())
                .to(deviceExchange())
                .with(deviceDataRoutingKey);
    }

    @Bean
    public Binding userSyncBinding() {
        return BindingBuilder.bind(syncQueue())
                .to(syncExchange())
                .with("user.#");
    }

    @Bean
    public Binding deviceSyncBinding() {
        return BindingBuilder.bind(syncQueue())
                .to(syncExchange())
                .with("device.#");
    }

    @Bean
    public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
        final RabbitTemplate rabbitTemplate = new RabbitTemplate(connectionFactory);
        rabbitTemplate.setMessageConverter(jsonMessageConverter());
        return rabbitTemplate;
    }
}