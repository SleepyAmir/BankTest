package com.springbank.config;

import org.springframework.amqp.core.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    public static final String EXCHANGE = "banking.exchange";

    @Bean
    public TopicExchange bankingExchange() {
        return ExchangeBuilder.topicExchange(EXCHANGE).durable(true).build();
    }

    @Bean
    public Queue auditQueue() {
        return QueueBuilder.durable("audit.queue").build();
    }

    @Bean
    public Queue notificationQueue() {
        return QueueBuilder.durable("notification.queue").build();
    }

    @Bean
    public Queue fraudQueue() {
        return QueueBuilder.durable("fraud.queue").build();
    }

    @Bean
    public Queue analyticsQueue() {
        return QueueBuilder.durable("analytics.queue").build();
    }

    @Bean
    public Binding auditBinding(Queue auditQueue, TopicExchange bankingExchange) {
        return BindingBuilder.bind(auditQueue).to(bankingExchange).with("audit.*");
    }

    @Bean
    public Binding notificationBinding(Queue notificationQueue, TopicExchange bankingExchange) {
        return BindingBuilder.bind(notificationQueue).to(bankingExchange).with("notification.*");
    }

    @Bean
    public Binding fraudBinding(Queue fraudQueue, TopicExchange bankingExchange) {
        return BindingBuilder.bind(fraudQueue).to(bankingExchange).with("transaction.*");
    }

    @Bean
    public Binding analyticsBinding(Queue analyticsQueue, TopicExchange bankingExchange) {
        return BindingBuilder.bind(analyticsQueue).to(bankingExchange).with("transaction.*");
    }
}
