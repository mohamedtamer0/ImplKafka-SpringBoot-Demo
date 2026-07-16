package com.tamer.ImplKafka_SpringBoot_Demo.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.tamer.ImplKafka_SpringBoot_Demo.kafka.JsonKafkaProducer;
import com.tamer.ImplKafka_SpringBoot_Demo.kafka.KafkaProducer;
import com.tamer.ImplKafka_SpringBoot_Demo.kafka.MessagePublishException;
import com.tamer.ImplKafka_SpringBoot_Demo.model.User;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest({MessageController.class, JsonMessageController.class, GlobalExceptionHandler.class})
class KafkaControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private KafkaProducer kafkaProducer;

    @MockitoBean
    private JsonKafkaProducer jsonKafkaProducer;

    @Test
    void publishesTextThroughThePreferredPostEndpoint() throws Exception {
        mockMvc.perform(post("/api/v1/kafka/messages")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"message":"hello Kafka"}
                                """))
                .andExpect(status().isOk())
                .andExpect(content().string("Message sent to the topic"));

        verify(kafkaProducer).sendMessage("hello Kafka");
    }

    @Test
    void keepsTheLegacyGetEndpointCompatible() throws Exception {
        mockMvc.perform(get("/api/v1/kafka/publish").param("message", "hello"))
                .andExpect(status().isOk())
                .andExpect(content().string("Message sent to the topic"));

        verify(kafkaProducer).sendMessage("hello");
    }

    @Test
    void rejectsBlankTextMessages() throws Exception {
        mockMvc.perform(post("/api/v1/kafka/messages")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"message":"   "}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.detail").value("Request validation failed"))
                .andExpect(jsonPath("$.errors[0].field").value("message"));
    }

    @Test
    void rejectsInvalidUsers() throws Exception {
        mockMvc.perform(post("/api/v1/kafka/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"id":0,"firstName":"","lastName":"Tamer"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("Request validation failed"));
    }

    @Test
    void rejectsMalformedJsonWithoutLeakingParserDetails() throws Exception {
        mockMvc.perform(post("/api/v1/kafka/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{not-json"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("Request body is missing or malformed"));
    }

    @Test
    void returnsServiceUnavailableWhenKafkaDoesNotAcknowledge() throws Exception {
        doThrow(new MessagePublishException("kafkalearn", new IllegalStateException("down")))
                .when(kafkaProducer).sendMessage("hello");

        mockMvc.perform(get("/api/v1/kafka/publish").param("message", "hello"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.detail").value("Kafka did not acknowledge the message"));
    }

    @Test
    void publishesAValidUser() throws Exception {
        mockMvc.perform(post("/api/v1/kafka/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"id":1,"firstName":"Mohamed","lastName":"Tamer"}
                                """))
                .andExpect(status().isOk())
                .andExpect(content().string("Json message sent to kafka topic"));

        verify(jsonKafkaProducer).sendMessage(any(User.class));
    }
}
