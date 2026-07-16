package com.tamer.ImplKafka_SpringBoot_Demo.kafka;

import com.tamer.ImplKafka_SpringBoot_Demo.config.KafkaDemoProperties;
import com.tamer.ImplKafka_SpringBoot_Demo.model.User;
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
public class JsonKafkaProducer {

    private static final Logger LOGGER = LoggerFactory.getLogger(JsonKafkaProducer.class);

    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final KafkaDemoProperties properties;

    public JsonKafkaProducer(KafkaTemplate<String, Object> kafkaTemplate, KafkaDemoProperties properties) {
        this.kafkaTemplate = kafkaTemplate;
        this.properties = properties;
    }

    public void sendMessage(User user) {
        String topic = properties.topics().user();
        String messageId = Integer.toString(user.getId());
        try {
            SendResult<String, Object> result = kafkaTemplate.send(topic, messageId, user)
                    .get(properties.sendTimeout().toMillis(), TimeUnit.MILLISECONDS);
            LOGGER.info(
                    "Published user message id={} topic={} partition={} offset={}",
                    messageId,
                    topic,
                    result.getRecordMetadata().partition(),
                    result.getRecordMetadata().offset());
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
