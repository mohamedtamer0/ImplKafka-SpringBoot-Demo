package com.tamer.ImplKafka_SpringBoot_Demo.kafka;

public class MessagePublishException extends RuntimeException {

    public MessagePublishException(String topic, Throwable cause) {
        super("Kafka did not acknowledge a message for topic " + topic, cause);
    }
}
