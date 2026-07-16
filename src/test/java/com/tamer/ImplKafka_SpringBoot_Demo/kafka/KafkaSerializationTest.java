package com.tamer.ImplKafka_SpringBoot_Demo.kafka;

import static org.assertj.core.api.Assertions.assertThat;

import com.tamer.ImplKafka_SpringBoot_Demo.model.User;
import org.junit.jupiter.api.Test;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.springframework.kafka.support.serializer.JsonSerializer;

class KafkaSerializationTest {

    @Test
    void userRoundTripsThroughKafkaJsonSerialization() {
        User expected = new User(7, "Grace", "Hopper");
        JsonSerializer<User> serializer = new JsonSerializer<>();
        JsonDeserializer<User> deserializer = new JsonDeserializer<>(User.class, false);
        deserializer.addTrustedPackages(User.class.getPackageName());

        byte[] bytes = serializer.serialize("users", expected);
        User actual = deserializer.deserialize("users", bytes);

        assertThat(actual).isEqualTo(expected);
    }
}
