package com.vio.load_balancing.config;

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

    @Value("${app.rabbitmq.exchange.device-data}")
    private String deviceDataExchange;

    @Value("${app.rabbitmq.routing-key.device-data}")
    private String deviceDataRoutingKey;

    @Value("${app.rabbitmq.exchange.ingest}")
    private String ingestExchange;

    @Value("${app.rabbitmq.routing-key.ingest-prefix}")
    private String ingestRoutingKeyPrefix;

    @Value("${app.monitoring.replicas:3}")
    private int totalReplicas;


    @Bean
    public Queue deviceDataQueue() {
        return new Queue(deviceDataQueue, true);
    }

    @Bean
    public TopicExchange deviceDataExchange() {
        return new TopicExchange(deviceDataExchange, true, false);
    }

    @Bean
    public Binding deviceDataBinding() {
        return BindingBuilder
                .bind(deviceDataQueue())
                .to(deviceDataExchange())
                .with(deviceDataRoutingKey);
    }

    @Bean
    public TopicExchange ingestExchange() {
        return new TopicExchange(ingestExchange, true, false);
    }

    @Bean
    public Declarables ingestQueuesAndBindings() {
        Declarable[] declarables = new Declarable[totalReplicas * 2]; // queues + bindings

        for (int i = 1; i <= totalReplicas; i++) {
            String queueName = "ingest.queue." + i;
            String routingKey = ingestRoutingKeyPrefix + i;

            Queue queue = new Queue(queueName, true);
            Binding binding = BindingBuilder
                    .bind(queue)
                    .to(ingestExchange())
                    .with(routingKey);

            declarables[(i - 1) * 2] = queue;
            declarables[(i - 1) * 2 + 1] = binding;
        }

        return new Declarables(declarables);
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