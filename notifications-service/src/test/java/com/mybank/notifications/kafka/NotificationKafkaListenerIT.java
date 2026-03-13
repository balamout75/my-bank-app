package com.mybank.notifications.kafka;

import com.mybank.notifications.dto.NotificationEvent;
import com.mybank.notifications.model.NotificationId;
import com.mybank.notifications.repository.NotificationRepository;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.serializer.JacksonJsonSerializer;
import org.springframework.kafka.test.EmbeddedKafkaBroker;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.context.ActiveProfiles;

import java.time.Duration;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

@SpringBootTest
@ActiveProfiles("kafka-test")
@EmbeddedKafka(
        topics = {"notifications"},
        bootstrapServersProperty = "spring.kafka.bootstrap-servers"
)
class NotificationKafkaListenerIT extends BaseIntegrationTest {

    @Autowired
    NotificationRepository notificationRepository;

    @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
    @Autowired
    EmbeddedKafkaBroker embeddedKafkaBroker;

    @BeforeEach
    void cleanUp() {
        notificationRepository.deleteAll();
    }

    //Kafka событие → уведомление сохраняется в БД
    @Test
    void shouldConsumeEventAndSaveNotification() {
        var event = new NotificationEvent(
                "cash-service", 99001L, "alice",
                Map.of("operation", "DEPOSIT", "amount", "500.00")
        );

        createProducer().send("notifications", event.operationId().toString(), event);

        await().atMost(Duration.ofSeconds(10)).untilAsserted(() -> {
            var notification = notificationRepository.findById(
                    new NotificationId("cash-service", 99001L)
            );
            assertThat(notification).isPresent();
            assertThat(notification.get().getUsername()).isEqualTo("alice");
            assertThat(notification.get().getPayload()).containsEntry("operation", "DEPOSIT");
        });
    }

    //Дублированное Kafka событие → в БД ровно одна запись (идемпотентность)
    @Test
    void shouldHandleDuplicateEvents() {
        var event = new NotificationEvent(
                "cash-service", 99002L, "bob",
                Map.of("operation", "WITHDRAW", "amount", "100.00")
        );

        var producer = createProducer();
        producer.send("notifications", event.operationId().toString(), event);
        producer.send("notifications", event.operationId().toString(), event);

        await().atMost(Duration.ofSeconds(10)).untilAsserted(() -> {
            var notification = notificationRepository.findById(
                    new NotificationId("cash-service", 99002L)
            );
            assertThat(notification).isPresent();
            assertThat(notification.get().getUsername()).isEqualTo("bob");
        });

        // Даём время на обработку дубля, проверяем что count не вырос
        await().during(Duration.ofSeconds(2)).untilAsserted(() ->
                assertThat(notificationRepository.findById(
                        new NotificationId("cash-service", 99002L)
                )).isPresent()
        );
    }

    private KafkaTemplate<String, NotificationEvent> createProducer() {
        var props = Map.<String, Object>of(
                ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, embeddedKafkaBroker.getBrokersAsString(),
                ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class
        );
        var serializer = new JacksonJsonSerializer<NotificationEvent>();

        return new KafkaTemplate<>(new DefaultKafkaProducerFactory<>(
                props, new StringSerializer(), serializer
        ));
    }
}