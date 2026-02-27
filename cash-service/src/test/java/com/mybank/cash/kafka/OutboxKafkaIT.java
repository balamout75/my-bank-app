package com.mybank.cash.kafka;

import com.mybank.cash.dto.CashOperationType;
import com.mybank.cash.dto.NotificationEvent;
import com.mybank.cash.dto.OperationStatus;
import com.mybank.cash.model.CashOperation;
import com.mybank.cash.outbox.OutboxProcessor;
import com.mybank.cash.repository.CashOperationRepository;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.support.serializer.JacksonJsonDeserializer;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Testcontainers
class OutboxKafkaIT {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:18.1")
            .withDatabaseName("mybank")
            .withUsername("mybank")
            .withPassword("mybank");

    @Container
    static KafkaContainer kafka = new KafkaContainer(
            DockerImageName.parse("apache/kafka:4.2.0")
    );

    @DynamicPropertySource
    static void configure(DynamicPropertyRegistry registry) {
        // PostgreSQL
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.liquibase.default-schema", () -> "cash");

        // Kafka
        registry.add("spring.kafka.bootstrap-servers", kafka::getBootstrapServers);
        registry.add("application.kafka.topic.notifications", () -> "notifications");

        // Отключаем Eureka и OAuth2
        registry.add("eureka.client.enabled", () -> "false");
        registry.add("spring.cloud.discovery.enabled", () -> "false");
        registry.add("spring.autoconfigure.exclude", () ->
                "org.springframework.boot.autoconfigure.security.oauth2.client.servlet.OAuth2ClientAutoConfiguration,"
                        + "org.springframework.boot.autoconfigure.security.oauth2.resource.servlet.OAuth2ResourceServerAutoConfiguration,"
                        + "org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration");
    }

    @Autowired
    CashOperationRepository repository;

    @Autowired
    OutboxProcessor outboxProcessor;

    @Test
    void shouldSendNotificationEventToKafka() {
        CashOperation op = CashOperation.builder()
                .operationId(repository.getNextOperationId())
                .username("alice")
                .amount(new BigDecimal("100.00"))
                .type(CashOperationType.DEPOSIT)
                .status(OperationStatus.UPDATED)
                .createdAt(LocalDateTime.now())
                .notificationAttemptsAt(LocalDateTime.now())
                .notificationAttempts(0)
                .build();
        repository.save(op);

        // When: OutboxProcessor отправляет в Kafka
        outboxProcessor.process();

        // Then: сообщение в топике
        try (KafkaConsumer<String, NotificationEvent> consumer = createConsumer()) {
            consumer.subscribe(List.of("notifications"));
            ConsumerRecords<String, NotificationEvent> records = consumer.poll(Duration.ofSeconds(10));

            assertThat(records.count()).isGreaterThanOrEqualTo(1);

            var record = records.iterator().next();
            assertThat(record.value().service()).isEqualTo("cash-service");
            assertThat(record.value().username()).isEqualTo("alice");
            assertThat(record.value().operationId()).isEqualTo(op.getOperationId());
        }

        // And: статус операции = NOTIFIED
        CashOperation updated = repository.findById(op.getOperationId()).orElseThrow();
        assertThat(updated.getStatus()).isEqualTo(OperationStatus.NOTIFIED);
    }

    private KafkaConsumer<String, NotificationEvent> createConsumer() {
        var props = Map.<String, Object>of(
                ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, kafka.getBootstrapServers(),
                ConsumerConfig.GROUP_ID_CONFIG, "test-group",
                ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest"
        );

        var valueDeserializer = new JacksonJsonDeserializer<>(NotificationEvent.class);
        valueDeserializer.addTrustedPackages("com.mybank"); // или "com.mybank.*"

        return new KafkaConsumer<>(props, new StringDeserializer(), valueDeserializer);
    }
}
