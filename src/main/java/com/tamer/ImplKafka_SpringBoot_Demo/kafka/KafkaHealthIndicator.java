package com.tamer.ImplKafka_SpringBoot_Demo.kafka;

import org.springframework.boot.actuate.health.AbstractHealthIndicator;
import org.springframework.boot.actuate.health.Health;
import org.springframework.kafka.core.KafkaAdmin;
import org.springframework.stereotype.Component;

@Component("kafka")
public class KafkaHealthIndicator extends AbstractHealthIndicator {

    private final KafkaAdmin kafkaAdmin;

    public KafkaHealthIndicator(KafkaAdmin kafkaAdmin) {
        this.kafkaAdmin = kafkaAdmin;
    }

    @Override
    protected void doHealthCheck(Health.Builder builder) {
        String clusterId = kafkaAdmin.clusterId();
        if (clusterId == null) {
            builder.down().withDetail("reason", "Kafka cluster ID is unavailable");
            return;
        }
        builder.up().withDetail("clusterId", clusterId);
    }
}
