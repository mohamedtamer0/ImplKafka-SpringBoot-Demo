package com.tamer.ImplKafka_SpringBoot_Demo.config;


import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class KafkaTopicConfig {

    @Bean
    public NewTopic kafkaLearnTopic(){
        return TopicBuilder.name("kafkalearn")
                .build();
    }


    @Bean
    public NewTopic kafkaLearnJsonTopic(){
        return TopicBuilder.name("kafkalearn_json")
                .build();
    }

}
