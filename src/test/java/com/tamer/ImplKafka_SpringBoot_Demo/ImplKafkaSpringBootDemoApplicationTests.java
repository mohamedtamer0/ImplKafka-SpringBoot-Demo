package com.tamer.ImplKafka_SpringBoot_Demo;

import static org.assertj.core.api.Assertions.assertThat;

import com.tamer.ImplKafka_SpringBoot_Demo.config.KafkaDemoProperties;
import com.tamer.ImplKafka_SpringBoot_Demo.kafka.JsonKafkaProducer;
import com.tamer.ImplKafka_SpringBoot_Demo.kafka.KafkaProducer;
import org.apache.kafka.clients.admin.NewTopic;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.ActiveProfiles;

@ActiveProfiles("test")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
class ImplKafkaSpringBootDemoApplicationTests {

    @Autowired
    private ApplicationContext applicationContext;

    @Autowired
    private KafkaDemoProperties properties;

    @Test
    void contextLoads() {
        assertThat(applicationContext.getBean(KafkaProducer.class)).isNotNull();
        assertThat(applicationContext.getBean(JsonKafkaProducer.class)).isNotNull();
        assertThat(properties.topics().text()).isEqualTo("kafkalearn");
        assertThat(properties.topics().user()).isEqualTo("kafkalearn_json");
        assertThat(applicationContext.getBeansOfType(NewTopic.class))
                .containsKeys("textTopic", "userTopic", "textDeadLetterTopic", "userDeadLetterTopic");
    }
}
