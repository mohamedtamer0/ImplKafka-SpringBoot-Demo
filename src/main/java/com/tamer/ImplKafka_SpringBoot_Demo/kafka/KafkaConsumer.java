package com.tamer.ImplKafka_SpringBoot_Demo.kafka;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Service
public class KafkaConsumer {

    private static final Logger LOGGER = LoggerFactory.getLogger(KafkaConsumer.class);

    @KafkaListener(
            topics = "${app.kafka.topics.text}",
            groupId = "${app.kafka.consumer-group}",
            containerFactory = "stringKafkaListenerContainerFactory")
    public void consume(ConsumerRecord<String, String> record) {
        if (record.value() == null) {
            LOGGER.info(
                    "Consumed text tombstone id={} topic={} partition={} offset={}",
                    record.key(), record.topic(), record.partition(), record.offset());
            return;
        }
        LOGGER.info(
                "Consumed text message id={} topic={} partition={} offset={} payloadLength={}",
                record.key(),
                record.topic(),
                record.partition(),
                record.offset(),
                record.value().length());
    }
}
