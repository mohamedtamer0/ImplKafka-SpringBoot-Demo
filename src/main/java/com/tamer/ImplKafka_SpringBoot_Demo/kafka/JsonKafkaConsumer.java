package com.tamer.ImplKafka_SpringBoot_Demo.kafka;

import com.tamer.ImplKafka_SpringBoot_Demo.model.User;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Service
public class JsonKafkaConsumer {
    private static final Logger LOGGER = LoggerFactory.getLogger(JsonKafkaConsumer.class);

    @KafkaListener(
            topics = "${app.kafka.topics.user}",
            groupId = "${app.kafka.consumer-group}",
            containerFactory = "userKafkaListenerContainerFactory")
    public void consume(ConsumerRecord<String, User> record) {
        if (record.value() == null) {
            LOGGER.info(
                    "Consumed user tombstone id={} topic={} partition={} offset={}",
                    record.key(), record.topic(), record.partition(), record.offset());
            return;
        }
        LOGGER.info(
                "Consumed user message id={} userId={} topic={} partition={} offset={}",
                record.key(),
                record.value().getId(),
                record.topic(),
                record.partition(),
                record.offset());
    }
}
