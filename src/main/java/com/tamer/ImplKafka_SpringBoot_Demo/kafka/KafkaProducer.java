package com.tamer.ImplKafka_SpringBoot_Demo.kafka;

import com.tamer.ImplKafka_SpringBoot_Demo.config.KafkaDemoProperties;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.apache.kafka.common.KafkaException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;

@Service
public class KafkaProducer {

    private static final Logger LOGGER = LoggerFactory.getLogger(KafkaProducer.class);

    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final KafkaDemoProperties properties;

    public KafkaProducer(KafkaTemplate<String, Object> kafkaTemplate, KafkaDemoProperties properties) {
        this.kafkaTemplate = kafkaTemplate;
        this.properties = properties;
    }

    public void sendMessage(String message) {
        String topic = properties.topics().text();
        String messageId = UUID.randomUUID().toString();
        try {
            SendResult<String, Object> result = kafkaTemplate.send(topic, messageId, message)
                    .get(properties.sendTimeout().toMillis(), TimeUnit.MILLISECONDS);
            LOGGER.info(
                    "Published text message id={} topic={} partition={} offset={} payloadLength={}",
                    messageId,
                    topic,
                    result.getRecordMetadata().partition(),
                    result.getRecordMetadata().offset(),
                    message.length());
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new MessagePublishException(topic, exception);
        } catch (ExecutionException | TimeoutException exception) {
            throw new MessagePublishException(topic, exception);
        } catch (KafkaException | org.springframework.kafka.KafkaException exception) {
            throw new MessagePublishException(topic, exception);
        }
    }
}
