package com.tamer.ImplKafka_SpringBoot_Demo.kafka;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doCallRealMethod;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.serialization.ByteArrayDeserializer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.util.TestPropertyValues;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.http.MediaType;
import org.springframework.kafka.config.KafkaListenerEndpointRegistry;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.test.EmbeddedKafkaBroker;
import org.springframework.kafka.test.EmbeddedKafkaKraftBroker;
import org.springframework.kafka.test.utils.KafkaTestUtils;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.web.servlet.MockMvc;

@AutoConfigureMockMvc
@DirtiesContext
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@ContextConfiguration(initializers = KafkaMessagingIT.KafkaBootstrapInitializer.class)
@SpringBootTest(properties = {
        "app.kafka.topics.text=integration-text",
        "app.kafka.topics.user=integration-user",
        "app.kafka.consumer-group=integration-app",
        "app.kafka.retry.max-attempts=3",
        "app.kafka.retry.backoff=50ms",
        "spring.kafka.admin.auto-create=true",
        "spring.kafka.listener.auto-startup=true"
})
class KafkaMessagingIT {

    private static final String USER_TOPIC = "integration-user";
    private static final String USER_DLT = "integration-user.dlt";
    private static final EmbeddedKafkaBroker EMBEDDED_KAFKA = startBroker();

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private KafkaTemplate<String, Object> kafkaTemplate;

    @Autowired
    private KafkaListenerEndpointRegistry listenerRegistry;

    @Autowired
    private ProducerFactory<String, Object> producerFactory;

    @MockitoSpyBean
    private KafkaConsumer textConsumer;

    @MockitoSpyBean
    private JsonKafkaConsumer userConsumer;

    @AfterAll
    void stopBroker() {
        listenerRegistry.stop();
        producerFactory.reset();
        EMBEDDED_KAFKA.destroy();
    }

    @Test
    void publishesAndConsumesTextEndToEnd() throws Exception {
        String message = "integration-" + UUID.randomUUID();

        mockMvc.perform(post("/api/v1/kafka/messages")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"message\":\"" + message + "\"}"))
                .andExpect(status().isOk());

        verify(textConsumer, timeout(10_000)).consume(any());
    }

    @Test
    void retriesListenerFailuresBeforeCommittingTheRecord() throws Exception {
        doThrow(new IllegalStateException("first attempt"))
                .doThrow(new IllegalStateException("second attempt"))
                .doCallRealMethod()
                .when(userConsumer).consume(any());

        mockMvc.perform(post("/api/v1/kafka/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"id":99,"firstName":"Retry","lastName":"Example"}
                                """))
                .andExpect(status().isOk());

        verify(userConsumer, timeout(10_000).times(3)).consume(any());
    }

    @Test
    void routesMalformedJsonToTheDeadLetterTopic() throws Exception {
        Map<String, Object> consumerProperties =
                KafkaTestUtils.consumerProps("dlt-verifier", "false", EMBEDDED_KAFKA);
        consumerProperties.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");

        try (Consumer<String, byte[]> dltConsumer = new DefaultKafkaConsumerFactory<>(
                consumerProperties, new StringDeserializer(), new ByteArrayDeserializer()).createConsumer()) {
            EMBEDDED_KAFKA.consumeFromAnEmbeddedTopic(dltConsumer, USER_DLT);
            byte[] malformedJson = "{not-json".getBytes(StandardCharsets.UTF_8);

            kafkaTemplate.send(USER_TOPIC, "malformed", malformedJson).get(10, TimeUnit.SECONDS);

            ConsumerRecord<String, byte[]> deadLetterRecord =
                    KafkaTestUtils.getSingleRecord(dltConsumer, USER_DLT, Duration.ofSeconds(10));
            assertThat(deadLetterRecord.key()).isEqualTo("malformed");
            assertThat(deadLetterRecord.value()).isEqualTo(malformedJson);
        }
    }

    @Test
    void actuatorReportsKafkaAsHealthy() throws Exception {
        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"))
                .andExpect(jsonPath("$.components.kafka.status").value("UP"));
    }

    private static EmbeddedKafkaBroker startBroker() {
        EmbeddedKafkaBroker broker = new EmbeddedKafkaKraftBroker(
                1,
                3,
                "integration-text",
                "integration-user",
                "integration-text.dlt",
                "integration-user.dlt")
                .brokerListProperty("spring.kafka.bootstrap-servers");
        try {
            broker.afterPropertiesSet();
            return broker;
        } catch (Exception exception) {
            throw new IllegalStateException("Could not start the embedded Kafka broker", exception);
        }
    }

    static final class KafkaBootstrapInitializer
            implements ApplicationContextInitializer<ConfigurableApplicationContext> {

        @Override
        public void initialize(ConfigurableApplicationContext applicationContext) {
            TestPropertyValues.of(
                            "spring.kafka.bootstrap-servers=" + EMBEDDED_KAFKA.getBrokersAsString())
                    .applyTo(applicationContext.getEnvironment());
        }
    }
}
