package com.tamer.ImplKafka_SpringBoot_Demo.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class KafkaTopicConfig {

    @Bean
    public NewTopic textTopic(KafkaDemoProperties properties) {
        return topic(properties.topics().text(), properties);
    }

    @Bean
    public NewTopic userTopic(KafkaDemoProperties properties) {
        return topic(properties.topics().user(), properties);
    }

    @Bean
    public NewTopic textDeadLetterTopic(KafkaDemoProperties properties) {
        return topic(properties.deadLetterTopic(properties.topics().text()), properties);
    }

    @Bean
    public NewTopic userDeadLetterTopic(KafkaDemoProperties properties) {
        return topic(properties.deadLetterTopic(properties.topics().user()), properties);
    }

    private NewTopic topic(String name, KafkaDemoProperties properties) {
        return TopicBuilder.name(name)
                .partitions(properties.partitions())
                .replicas(properties.replicationFactor())
                .build();
    }
}
