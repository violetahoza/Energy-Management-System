package com.vio.customer_support.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    public static final String OVERCONSUMPTION_EXCHANGE = "overconsumption.exchange";
    public static final String OVERCONSUMPTION_QUEUE = "overconsumption.alert.queue";
    public static final String OVERCONSUMPTION_ROUTING_KEY = "overconsumption.alert";

    @Bean
    public TopicExchange overconsumptionExchange() {
        return new TopicExchange(OVERCONSUMPTION_EXCHANGE, true, false);
    }

    @Bean
    public Queue overconsumptionQueue() {
        return new Queue(OVERCONSUMPTION_QUEUE, true);
    }

    @Bean
    public Binding overconsumptionBinding() {
        return BindingBuilder
                .bind(overconsumptionQueue())
                .to(overconsumptionExchange())
                .with(OVERCONSUMPTION_ROUTING_KEY);
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