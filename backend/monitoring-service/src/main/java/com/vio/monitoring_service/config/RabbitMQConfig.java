package com.vio.monitoring_service.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.amqp.RabbitProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

@Configuration
public class RabbitMQConfig {

    public static final String DEVICE_SYNC_EXCHANGE = "device.sync.exchange";
    public static final String DEVICE_SYNC_QUEUE_MONITORING = "device.sync.queue.monitoring";
    public static final String DEVICE_SYNC_ROUTING_KEY = "device.sync";

    public static final String DEVICE_DATA_QUEUE = "device.data.queue";
    public static final String DEVICE_DATA_EXCHANGE = "device.data.exchange";
    public static final String DEVICE_DATA_ROUTING_KEY = "device.data";


    // Connection Factory for synchronization-broker (device sync messages)
    @Bean
    @Primary
    @ConfigurationProperties(prefix = "spring.rabbitmq.sync")
    public RabbitProperties syncRabbitProperties() {
        return new RabbitProperties();
    }

    @Bean
    @Primary
    public ConnectionFactory syncConnectionFactory() {
        CachingConnectionFactory connectionFactory = new CachingConnectionFactory();
        RabbitProperties props = syncRabbitProperties();
        connectionFactory.setHost(props.getHost());
        connectionFactory.setPort(props.getPort());
        connectionFactory.setUsername(props.getUsername());
        connectionFactory.setPassword(props.getPassword());
        return connectionFactory;
    }

    // Connection Factory for data-collection-broker (device measurements)
    @Bean
    @ConfigurationProperties(prefix = "spring.rabbitmq.data")
    public RabbitProperties dataRabbitProperties() {
        return new RabbitProperties();
    }

    @Bean
    public ConnectionFactory dataConnectionFactory() {
        CachingConnectionFactory connectionFactory = new CachingConnectionFactory();
        RabbitProperties props = dataRabbitProperties();
        connectionFactory.setHost(props.getHost());
        connectionFactory.setPort(props.getPort());
        connectionFactory.setUsername(props.getUsername());
        connectionFactory.setPassword(props.getPassword());
        return connectionFactory;
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
    public Binding deviceSyncBinding() {
        return BindingBuilder
                .bind(deviceSyncQueueMonitoring())
                .to(deviceSyncExchange())
                .with(DEVICE_SYNC_ROUTING_KEY);
    }

    @Bean
    public Queue deviceDataQueue() {
        return new Queue(DEVICE_DATA_QUEUE, true);
    }

    @Bean
    public TopicExchange deviceDataExchange() {
        return new TopicExchange(DEVICE_DATA_EXCHANGE, true, false);
    }

    @Bean
    public Binding deviceDataBinding() {
        return BindingBuilder
                .bind(deviceDataQueue())
                .to(deviceDataExchange())
                .with(DEVICE_DATA_ROUTING_KEY);
    }

    @Bean
    public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    @Primary
    public RabbitTemplate syncRabbitTemplate(@Qualifier("syncConnectionFactory") ConnectionFactory connectionFactory) {
        RabbitTemplate rabbitTemplate = new RabbitTemplate(connectionFactory);
        rabbitTemplate.setMessageConverter(jsonMessageConverter());
        return rabbitTemplate;
    }

    @Bean
    public RabbitTemplate dataRabbitTemplate(@Qualifier("dataConnectionFactory") ConnectionFactory connectionFactory) {
        RabbitTemplate rabbitTemplate = new RabbitTemplate(connectionFactory);
        rabbitTemplate.setMessageConverter(jsonMessageConverter());
        return rabbitTemplate;
    }
}