package com.springbank.transaction.read.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    public static final String TRANSACTION_QUEUE = "transaction.read.queue";
    public static final String EXCHANGE = "banking.exchange";
    public static final String ROUTING_KEY = "transaction.completed";

    @Bean
    public Queue transactionReadQueue() {
        return new Queue(TRANSACTION_QUEUE, true);
    }

    @Bean
    public TopicExchange bankingExchange() {
        return new TopicExchange(EXCHANGE);
    }

    @Bean
    public Binding transactionReadBinding(Queue transactionReadQueue, TopicExchange bankingExchange) {
        return BindingBuilder.bind(transactionReadQueue).to(bankingExchange).with(ROUTING_KEY);
    }

    @Bean
    public Jackson2JsonMessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }
}
