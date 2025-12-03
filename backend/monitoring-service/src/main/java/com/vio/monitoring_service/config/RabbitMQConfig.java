package com.vio.monitoring_service.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

@Configuration
public class RabbitMQConfig {
    public static final String DEVICE_SYNC_EXCHANGE = "device.sync.exchange";
    public static final String DEVICE_SYNC_QUEUE_MONITORING = "device.sync.queue.monitoring";
    public static final String DEVICE_SYNC_ROUTING_KEY = "device.sync";

    public static final String OVERCONSUMPTION_EXCHANGE = "overconsumption.exchange";
    public static final String OVERCONSUMPTION_QUEUE = "overconsumption.alert.queue";
    public static final String OVERCONSUMPTION_ROUTING_KEY = "overconsumption.alert";

    public static final String INGEST_EXCHANGE = "ingest.exchange";

    @Value("${spring.rabbitmq.sync.host}")
    private String syncHost;

    @Value("${spring.rabbitmq.sync.port}")
    private int syncPort;

    @Value("${spring.rabbitmq.sync.username}")
    private String syncUsername;

    @Value("${spring.rabbitmq.sync.password}")
    private String syncPassword;

    @Value("${spring.rabbitmq.data.host}")
    private String dataHost;

    @Value("${spring.rabbitmq.data.port}")
    private int dataPort;

    @Value("${spring.rabbitmq.data.username}")
    private String dataUsername;

    @Value("${spring.rabbitmq.data.password}")
    private String dataPassword;

    @Value("${app.replica.id:1}")
    private int replicaId;

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

    @Bean(name = "alertRabbitTemplate")
    public RabbitTemplate alertRabbitTemplate(@Qualifier("syncConnectionFactory") ConnectionFactory connectionFactory) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(jsonMessageConverter());
        return template;
    }

    @Bean(name = "syncConnectionFactory")
    @Primary
    public ConnectionFactory syncConnectionFactory() {
        CachingConnectionFactory factory = new CachingConnectionFactory();
        factory.setHost(syncHost);
        factory.setPort(syncPort);
        factory.setUsername(syncUsername);
        factory.setPassword(syncPassword);
        return factory;
    }

    @Bean(name = "syncRabbitTemplate")
    @Primary
    public RabbitTemplate syncRabbitTemplate(@Qualifier("syncConnectionFactory") ConnectionFactory connectionFactory) {
        RabbitTemplate rabbitTemplate = new RabbitTemplate(connectionFactory);
        rabbitTemplate.setMessageConverter(jsonMessageConverter());
        return rabbitTemplate;
    }

    @Bean(name = "syncListenerContainerFactory")
    @Primary
    public SimpleRabbitListenerContainerFactory syncListenerContainerFactory(
            @Qualifier("syncConnectionFactory") ConnectionFactory connectionFactory) {
        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory);
        factory.setMessageConverter(jsonMessageConverter());
        return factory;
    }

    @Bean
    public Queue deviceSyncQueueMonitoring() {
        return new Queue(DEVICE_SYNC_QUEUE_MONITORING, true);
    }

    @Bean
    public TopicExchange deviceSyncExchange() {
        return new TopicExchange(DEVICE_SYNC_EXCHANGE, true, false);
    }

    @Bean
    public Binding deviceSyncBinding() {
        return BindingBuilder
                .bind(deviceSyncQueueMonitoring())
                .to(deviceSyncExchange())
                .with(DEVICE_SYNC_ROUTING_KEY);
    }

    @Bean(name = "dataConnectionFactory")
    public ConnectionFactory dataConnectionFactory() {
        CachingConnectionFactory factory = new CachingConnectionFactory();
        factory.setHost(dataHost);
        factory.setPort(dataPort);
        factory.setUsername(dataUsername);
        factory.setPassword(dataPassword);
        return factory;
    }

    @Bean(name = "dataRabbitTemplate")
    public RabbitTemplate dataRabbitTemplate(@Qualifier("dataConnectionFactory") ConnectionFactory connectionFactory) {
        RabbitTemplate rabbitTemplate = new RabbitTemplate(connectionFactory);
        rabbitTemplate.setMessageConverter(jsonMessageConverter());
        return rabbitTemplate;
    }

    @Bean(name = "dataListenerContainerFactory")
    public SimpleRabbitListenerContainerFactory dataListenerContainerFactory(@Qualifier("dataConnectionFactory") ConnectionFactory connectionFactory) {
        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory);
        factory.setMessageConverter(jsonMessageConverter());
        return factory;
    }

    @Bean
    public Queue ingestQueue() {
        String queueName = "ingest.queue." + replicaId;
        return new Queue(queueName, true);
    }

    @Bean
    public TopicExchange ingestExchange() {
        return new TopicExchange(INGEST_EXCHANGE, true, false);
    }

    @Bean
    public Binding ingestBinding() {
        String routingKey = "ingest.data." + replicaId;
        return BindingBuilder
                .bind(ingestQueue())
                .to(ingestExchange())
                .with(routingKey);
    }

    @Bean
    public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }
}