package com.tamer.ImplKafka_SpringBoot_Demo.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tamer.ImplKafka_SpringBoot_Demo.model.User;
import java.util.Map;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.kafka.KafkaProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.listener.CommonErrorHandler;
import org.springframework.kafka.listener.ContainerProperties;
import org.springframework.kafka.support.serializer.ErrorHandlingDeserializer;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.springframework.validation.Validator;

@Configuration
public class KafkaConsumerConfig {

    @Bean
    public ConsumerFactory<String, String> stringConsumerFactory(KafkaProperties kafkaProperties) {
        Map<String, Object> properties = kafkaProperties.buildConsumerProperties(null);
        return new DefaultKafkaConsumerFactory<>(
                properties,
                new ErrorHandlingDeserializer<>(new StringDeserializer()),
                new ErrorHandlingDeserializer<>(new StringDeserializer()));
    }

    @Bean
    public ConsumerFactory<String, User> userConsumerFactory(
            KafkaProperties kafkaProperties,
            ObjectMapper objectMapper,
            Validator validator) {
        JsonDeserializer<User> delegate = new JsonDeserializer<>(User.class, objectMapper, false);
        delegate.addTrustedPackages(User.class.getPackageName());

        ErrorHandlingDeserializer<User> valueDeserializer = new ErrorHandlingDeserializer<>(delegate);
        valueDeserializer.setValidator(validator);

        return new DefaultKafkaConsumerFactory<>(
                kafkaProperties.buildConsumerProperties(null),
                new ErrorHandlingDeserializer<>(new StringDeserializer()),
                valueDeserializer);
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, String> stringKafkaListenerContainerFactory(
            ConsumerFactory<String, String> stringConsumerFactory,
            CommonErrorHandler kafkaErrorHandler,
            KafkaDemoProperties properties,
            @Value("${spring.kafka.listener.auto-startup:true}") boolean autoStartup,
            @Value("${spring.kafka.listener.missing-topics-fatal:true}") boolean missingTopicsFatal) {
        ConcurrentKafkaListenerContainerFactory<String, String> factory =
                new ConcurrentKafkaListenerContainerFactory<>();
        configure(factory, stringConsumerFactory, kafkaErrorHandler, properties, autoStartup, missingTopicsFatal);
        return factory;
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, User> userKafkaListenerContainerFactory(
            ConsumerFactory<String, User> userConsumerFactory,
            CommonErrorHandler kafkaErrorHandler,
            KafkaDemoProperties properties,
            @Value("${spring.kafka.listener.auto-startup:true}") boolean autoStartup,
            @Value("${spring.kafka.listener.missing-topics-fatal:true}") boolean missingTopicsFatal) {
        ConcurrentKafkaListenerContainerFactory<String, User> factory =
                new ConcurrentKafkaListenerContainerFactory<>();
        configure(factory, userConsumerFactory, kafkaErrorHandler, properties, autoStartup, missingTopicsFatal);
        return factory;
    }

    private <V> void configure(
            ConcurrentKafkaListenerContainerFactory<String, V> factory,
            ConsumerFactory<String, V> consumerFactory,
            CommonErrorHandler kafkaErrorHandler,
            KafkaDemoProperties properties,
            boolean autoStartup,
            boolean missingTopicsFatal) {
        factory.setConsumerFactory(consumerFactory);
        factory.setCommonErrorHandler(kafkaErrorHandler);
        factory.setConcurrency(properties.concurrency());
        factory.setAutoStartup(autoStartup);
        factory.getContainerProperties().setAckMode(ContainerProperties.AckMode.RECORD);
        factory.getContainerProperties().setMissingTopicsFatal(missingTopicsFatal);
    }
}
