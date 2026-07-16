package com.tamer.ImplKafka_SpringBoot_Demo.config;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "app.kafka")
public record KafkaDemoProperties(
        @Valid @NotNull Topics topics,
        @NotBlank String consumerGroup,
        @Min(1) int concurrency,
        @Min(1) int partitions,
        @Min(1) @Max(Short.MAX_VALUE) short replicationFactor,
        @NotBlank String deadLetterSuffix,
        @NotNull Duration sendTimeout,
        @Valid @NotNull Retry retry) {

    public String deadLetterTopic(String topic) {
        return topic + deadLetterSuffix;
    }

    public record Topics(@NotBlank String text, @NotBlank String user) {
    }

    public record Retry(@Min(1) int maxAttempts, @NotNull Duration backoff) {
    }
}
