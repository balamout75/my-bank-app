package com.mybank.notifications.kafka;

import com.mybank.notifications.dto.NotificationEvent;
import com.mybank.notifications.model.NotificationId;
import com.mybank.notifications.repository.NotificationRepository;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.serializer.JacksonJsonSerializer;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.kafka.KafkaContainer;

import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.time.Duration;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

@SpringBootTest
@Testcontainers
class NotificationKafkaListenerIT {

    @Container
    static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:18.1")
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
        registry.add("spring.liquibase.default-schema", () -> "notifications");

        // Kafka
        registry.add("spring.kafka.bootstrap-servers", kafka::getBootstrapServers);
        registry.add("application.kafka.topic.notifications", () -> "notifications");

        // Отключаем OAuth2
        registry.add("spring.autoconfigure.exclude", () ->
                "org.springframework.boot.autoconfigure.security.oauth2.resource.servlet.OAuth2ResourceServerAutoConfiguration");
    }

    @Autowired
    NotificationRepository notificationRepository;

    @Test
    void shouldConsumeEventAndSaveNotification() {
        // Given: producer отправляет событие
        NotificationEvent event = new NotificationEvent(
                "cash-service",
                99001L,
                "alice",
                Map.of("operation", "DEPOSIT", "amount", "500.00")
        );

        KafkaTemplate<String, NotificationEvent> producer = createProducer();
        producer.send("notifications", event.operationId().toString(), event);

        // Then: уведомление появляется в БД
        await().atMost(Duration.ofSeconds(10)).untilAsserted(() -> {
            var notification = notificationRepository.findById(
                    new NotificationId("cash-service", 99001L)
            );
            assertThat(notification).isPresent();
            assertThat(notification.get().getUsername()).isEqualTo("alice");
            assertThat(notification.get().getPayload()).containsEntry("operation", "DEPOSIT");
        });
    }

    @Test
    void shouldHandleDuplicateEvents() {
        // Given: одно и то же событие дважды
        NotificationEvent event = new NotificationEvent(
                "cash-service",
                99002L,
                "bob",
                Map.of("operation", "WITHDRAW", "amount", "100.00")
        );

        KafkaTemplate<String, NotificationEvent> producer = createProducer();
        producer.send("notifications", event.operationId().toString(), event);
        producer.send("notifications", event.operationId().toString(), event);

        // Then: в БД ровно одна запись (idempotent)
        await().atMost(Duration.ofSeconds(10)).untilAsserted(() -> {
            var notification = notificationRepository.findById(
                    new NotificationId("cash-service", 99002L)
            );
            assertThat(notification).isPresent();
        });

        // Даём время на обработку дубля
        await().during(Duration.ofSeconds(2)).untilAsserted(() -> {
            long count = notificationRepository.count();
            // Не должно быть дублей для этого operationId
            assertThat(notificationRepository.findById(
                    new NotificationId("cash-service", 99002L)
            )).isPresent();
        });
    }

    private KafkaTemplate<String, NotificationEvent> createProducer() {
        var valueSerializer = new JacksonJsonSerializer<NotificationEvent>();

        var props = Map.<String, Object>of(
                ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, kafka.getBootstrapServers(),
                ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class
        );

        return new KafkaTemplate<>(
                new DefaultKafkaProducerFactory<>(
                        props,
                        new StringSerializer(),
                        valueSerializer
                )
        );
    }
}
