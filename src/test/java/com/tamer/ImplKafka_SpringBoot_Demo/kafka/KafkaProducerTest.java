package com.tamer.ImplKafka_SpringBoot_Demo.kafka;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.tamer.ImplKafka_SpringBoot_Demo.config.KafkaDemoProperties;
import com.tamer.ImplKafka_SpringBoot_Demo.model.User;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.apache.kafka.common.KafkaException;
import org.apache.kafka.common.TopicPartition;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;

@ExtendWith(MockitoExtension.class)
class KafkaProducerTest {

    @Mock
    private KafkaTemplate<String, Object> kafkaTemplate;

    private KafkaProducer textProducer;
    private JsonKafkaProducer userProducer;

    @BeforeEach
    void setUp() {
        KafkaDemoProperties properties = new KafkaDemoProperties(
                new KafkaDemoProperties.Topics("text-topic", "user-topic"),
                "test-group",
                1,
                3,
                (short) 1,
                ".dlt",
                Duration.ofSeconds(1),
                new KafkaDemoProperties.Retry(3, Duration.ofMillis(10)));
        textProducer = new KafkaProducer(kafkaTemplate, properties);
        userProducer = new JsonKafkaProducer(kafkaTemplate, properties);
    }

    @Test
    void textProducerSendsAStableMessageKeyAndWaitsForAcknowledgment() {
        when(kafkaTemplate.send(eq("text-topic"), anyString(), eq("hello")))
                .thenReturn(CompletableFuture.completedFuture(sendResult("text-topic", "hello")));

        textProducer.sendMessage("hello");

        verify(kafkaTemplate).send(eq("text-topic"), anyString(), eq("hello"));
    }

    @Test
    void userProducerUsesTheUserIdAsMessageKey() {
        User user = new User(42, "Mohamed", "Tamer");
        when(kafkaTemplate.send("user-topic", "42", user))
                .thenReturn(CompletableFuture.completedFuture(sendResult("user-topic", user)));

        userProducer.sendMessage(user);

        verify(kafkaTemplate).send("user-topic", "42", user);
    }

    @Test
    void producerSurfacesBrokerFailures() {
        CompletableFuture<SendResult<String, Object>> failure = new CompletableFuture<>();
        failure.completeExceptionally(new IllegalStateException("broker unavailable"));
        when(kafkaTemplate.send(eq("text-topic"), anyString(), eq("hello"))).thenReturn(failure);

        assertThatThrownBy(() -> textProducer.sendMessage("hello"))
                .isInstanceOf(MessagePublishException.class)
                .hasMessageContaining("text-topic");
    }

    @Test
    void producerSurfacesSynchronousKafkaFailures() {
        when(kafkaTemplate.send(eq("text-topic"), anyString(), eq("hello")))
                .thenThrow(new KafkaException("metadata unavailable"));

        assertThatThrownBy(() -> textProducer.sendMessage("hello"))
                .isInstanceOf(MessagePublishException.class)
                .hasCauseInstanceOf(KafkaException.class);
    }

    @Test
    void producerSurfacesKafkaTemplateFailures() {
        when(kafkaTemplate.send(eq("text-topic"), anyString(), eq("hello")))
                .thenThrow(new org.springframework.kafka.KafkaException("send failed"));

        assertThatThrownBy(() -> textProducer.sendMessage("hello"))
                .isInstanceOf(MessagePublishException.class)
                .hasCauseInstanceOf(org.springframework.kafka.KafkaException.class);
    }

    private SendResult<String, Object> sendResult(String topic, Object value) {
        ProducerRecord<String, Object> record = new ProducerRecord<>(topic, "key", value);
        RecordMetadata metadata = new RecordMetadata(
                new TopicPartition(topic, 0), 0, 0, 0, 0, 0);
        return new SendResult<>(record, metadata);
    }
}
