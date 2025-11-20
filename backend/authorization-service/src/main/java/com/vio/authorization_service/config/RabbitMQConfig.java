package com.vio.authorization_service.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@Slf4j
public class RabbitMQConfig {

    @Value("${app.rabbitmq.exchange.sync:sync.exchange}")
    private String syncExchange;

    @Value("${app.rabbitmq.queue.sync:sync.queue}")
    private String syncQueue;

    @Value("${app.rabbitmq.routing-key.user-sync:user.sync}")
    private String userSyncRoutingKey;

    @Bean
    public TopicExchange syncExchange() {
        log.info("AUTH - Creating TopicExchange: {}", syncExchange);
        return new TopicExchange(syncExchange);
    }

    @Bean
    public Queue syncQueue() {
        log.info("AUTH - Creating Queue: {}", syncQueue);
        return QueueBuilder.durable(syncQueue).build();
    }

    @Bean
    public Binding syncBinding(Queue syncQueue, TopicExchange syncExchange) {
        log.info("AUTH - Creating Binding: queue={}, exchange={}, routingKey={}",
                syncQueue.getName(), syncExchange.getName(), userSyncRoutingKey);
        return BindingBuilder
                .bind(syncQueue)
                .to(syncExchange)
                .with(userSyncRoutingKey);
    }

    @Bean
    public MessageConverter jsonMessageConverter() {
        log.info("AUTH - Creating Jackson2JsonMessageConverter");
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
        log.info("AUTH - Creating RabbitTemplate");
        RabbitTemplate rabbitTemplate = new RabbitTemplate(connectionFactory);
        rabbitTemplate.setMessageConverter(jsonMessageConverter());
        return rabbitTemplate;
    }
}