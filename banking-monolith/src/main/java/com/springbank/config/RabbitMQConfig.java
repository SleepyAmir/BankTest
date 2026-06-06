package com.springbank.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.rabbit.listener.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.support.converter.DefaultClassMapper;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.boot.autoconfigure.amqp.RabbitListenerContainerFactoryCustomizer;
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
    public Queue transactionReadQueue() {
        return QueueBuilder.durable("transaction.read.queue").build();
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

    @Bean
    public Binding transactionReadBinding(Queue transactionReadQueue, TopicExchange bankingExchange) {
        return BindingBuilder.bind(transactionReadQueue).to(bankingExchange).with("transaction.*");
    }

    @Bean
    public Binding notificationTransactionBinding(Queue notificationQueue, TopicExchange bankingExchange) {
        return BindingBuilder.bind(notificationQueue).to(bankingExchange).with("transaction.*");
    }

    @Bean
    public Binding notificationLoanBinding(Queue notificationQueue, TopicExchange bankingExchange) {
        return BindingBuilder.bind(notificationQueue).to(bankingExchange).with("loan.*");
    }

    @Bean
    public Jackson2JsonMessageConverter jsonMessageConverter() {
        Jackson2JsonMessageConverter converter = new Jackson2JsonMessageConverter();
        DefaultClassMapper classMapper = new DefaultClassMapper();
        classMapper.setTrustedPackages("com.springbank.common.event", "java.util", "java.time");
        converter.setClassMapper(classMapper);
        return converter;
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory, MessageConverter jsonMessageConverter) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(jsonMessageConverter);
        return template;
    }

    @Bean
    public RabbitListenerContainerFactoryCustomizer<SimpleRabbitListenerContainerFactory> rabbitListenerContainerFactoryCustomizer(MessageConverter jsonMessageConverter) {
        return factory -> factory.setMessageConverter(jsonMessageConverter);
    }
}
