package com.vio.device_service.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    public static final String USER_SYNC_EXCHANGE = "user.sync.exchange";
    public static final String USER_SYNC_QUEUE_DEVICE = "user.sync.queue.device";
    public static final String USER_SYNC_ROUTING_KEY = "user.sync";

    public static final String DEVICE_SYNC_EXCHANGE = "device.sync.exchange";
    public static final String DEVICE_SYNC_QUEUE_MONITORING = "device.sync.queue.monitoring";
    public static final String DEVICE_SYNC_ROUTING_KEY = "device.sync";

    @Bean
    public TopicExchange userSyncExchange() {
        return new TopicExchange(USER_SYNC_EXCHANGE, true, false);
    }

    @Bean
    public Queue userSyncQueueDevice() {
        return new Queue(USER_SYNC_QUEUE_DEVICE, true);
    }

    @Bean
    public Binding userSyncBindingDevice(Queue userSyncQueueDevice, TopicExchange userSyncExchange) {
        return BindingBuilder
                .bind(userSyncQueueDevice)
                .to(userSyncExchange)
                .with(USER_SYNC_ROUTING_KEY);
    }

    @Bean
    public TopicExchange deviceSyncExchange() {
        return new TopicExchange(DEVICE_SYNC_EXCHANGE, true, false);
    }

    @Bean
    public Queue deviceSyncQueueMonitoring() {
        return new Queue(DEVICE_SYNC_QUEUE_MONITORING, true);
    }

    @Bean
    public Binding deviceSyncBindingMonitoring() {
        return BindingBuilder
                .bind(deviceSyncQueueMonitoring())
                .to(deviceSyncExchange())
                .with(DEVICE_SYNC_ROUTING_KEY);
    }

    @Bean
    public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate rabbitTemplate = new RabbitTemplate(connectionFactory);
        rabbitTemplate.setMessageConverter(jsonMessageConverter());
        return rabbitTemplate;
    }
}