package com.tamer.ImplKafka_SpringBoot_Demo.controller;

import com.tamer.ImplKafka_SpringBoot_Demo.kafka.KafkaProducer;
import com.tamer.ImplKafka_SpringBoot_Demo.model.TextMessageRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/kafka")
public class MessageController {

    private final KafkaProducer kafkaProducer;

    public MessageController(KafkaProducer kafkaProducer) {
        this.kafkaProducer = kafkaProducer;
    }

    @GetMapping("/publish")
    public ResponseEntity<String> publish(
            @RequestParam("message") @NotBlank @Size(max = 1_000) String message) {
        kafkaProducer.sendMessage(message);
        return ResponseEntity.ok("Message sent to the topic");
    }

    @PostMapping("/messages")
    public ResponseEntity<String> publish(@Valid @RequestBody TextMessageRequest request) {
        kafkaProducer.sendMessage(request.message());
        return ResponseEntity.ok("Message sent to the topic");
    }
}
