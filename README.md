# ImplKafka-SpringBoot-Demo

## Overview
This is a Spring Boot demo project that integrates with Kafka for message publishing and consumption. The project demonstrates both string and JSON message handling with Kafka, utilizing REST APIs for interaction.

## Features
- Publish string and JSON messages to Kafka using REST API.
- Consume string and JSON messages from Kafka topics.
- Kafka configuration using Spring Boot.
- Support for message serialization and deserialization.

## Technologies Used
- Java
- Spring Boot
- Kafka
- Maven

## Project Structure
```
ImplKafka-SpringBoot-Demo
│── mvnw/
│── mvnw.cmd
│── pom.xml
│── src/
│ ├── main/
│ │ ├── java/com/tamer/ImplKafka_SpringBoot_Demo/
│ │ │ ├── config/
│ │ │ │ ├── KafkaTopicConfig.java
│ │ │ ├── controller/
│ │ │ │ ├── MessageController.java
│ │ │ │ ├── JsonMessageController.java
│ │ │ ├── kafka/
│ │ │ │ ├── KafkaConsumer.java
│ │ │ │ ├── JsonKafkaConsumer.java
│ │ │ │ ├── KafkaProducer.java
│ │ │ │ ├── JsonKafkaProducer.java
│ │ │ ├── model/
│ │ │ │ ├── User.java
│ │ │ ├── ImplKafkaSpringBootDemoApplication.java
│ ├── resources/
│ │ ├── application.properties
│── test/java/com/tamer/ImplKafka_SpringBoot_Demo/
│ ├── ImplKafkaSpringBootDemoApplicationTests.java
│── .gitattributes
│── .gitignore
│── LICENSE
│── README.md
```


## Configuration
Edit the `application.properties` file to configure Kafka settings:

```properties
spring.kafka.consumer.bootstrap-servers=localhost:9092
spring.kafka.consumer.group-id=myGroup
spring.kafka.consumer.auto-offset-reset=earliest
spring.kafka.consumer.key-deserializer=org.apache.kafka.common.serialization.StringDeserializer
spring.kafka.consumer.value-deserializer=org.springframework.kafka.support.serializer.JsonDeserializer
spring.kafka.consumer.properties.spring.json.trusted.packages=*

spring.kafka.producer.bootstrap-servers=localhost:9092
spring.kafka.producer.key-serializer=org.apache.kafka.common.serialization.StringSerializer
spring.kafka.producer.value-serializer=org.springframework.kafka.support.serializer.JsonSerializer

spring.kafka.topic.name=kafkalearn
spring.kafka.topic-json.name=kafkalearn_json
```

## Running the Application
1. Ensure RabbitMQ is running on your machine.
2. Clone the repository:
   ```sh
    git clone https://github.com/your-repo/ImplKafka-SpringBoot-Demo.git
   ```
3. Navigate to the project directory:
   ```sh
   cd ImplKafka-SpringBoot-Demo
   ```
4. Build the project using Maven:
   ```sh
   ./mvnw clean install
   ```
5. Run the Spring Boot application:
   ```sh
   ./mvnw spring-boot:run
   ```

## API Endpoints
### Publish String Message
- **Endpoint:** `GET http://localhost:8081/api/v1/kafka/publish?message=hello world`
- **Request Body:**
- **Response:**
  ```
  "Message sent to the topic"
  ```

### Publish JSON Message
- **Endpoint:** `POST http://localhost:8081/api/v1/kafka/publish`
- **Request Body:**
  ```json
  {
      "id": 1,
      "firstName": "Mohamed",
      "lastName": "Tamer"
  }
  ```
- **Response:**
  ```
  "Json message sent to kafka topic"
  ```

## License
This project is licensed under the MIT License.
