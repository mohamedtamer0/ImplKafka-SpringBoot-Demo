package com.tamer.ImplKafka_SpringBoot_Demo.model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record TextMessageRequest(
        @NotBlank @Size(max = 1_000) String message) {
}
