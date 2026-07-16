package com.tamer.ImplKafka_SpringBoot_Demo;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class ImplKafkaSpringBootDemoApplication {

    public static void main(String[] args) {
        SpringApplication.run(ImplKafkaSpringBootDemoApplication.class, args);
    }

}
