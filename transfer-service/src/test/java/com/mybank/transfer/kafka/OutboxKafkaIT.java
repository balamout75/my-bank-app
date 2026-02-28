package com.mybank.transfer.kafka;

import com.mybank.transfer.config.TestSecurityItConfig;
import com.mybank.transfer.dto.NotificationEvent;
import com.mybank.transfer.dto.OperationStatus;
import com.mybank.transfer.model.TransferOperation;
import com.mybank.transfer.repository.TransferOperationRepository;
import com.mybank.transfer.template.BaseIntegrationTest;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.support.serializer.JacksonJsonDeserializer;
import org.springframework.kafka.test.EmbeddedKafkaBroker;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.kafka.test.utils.KafkaTestUtils;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("kafka-test")
@Import(TestSecurityItConfig.class)
@EmbeddedKafka(
        topics = {"notifications"},
        bootstrapServersProperty = "spring.kafka.bootstrap-servers"
)
class OutboxKafkaIT extends BaseIntegrationTest {

    @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
    @Autowired
    EmbeddedKafkaBroker embeddedKafkaBroker;

    @Autowired
    TransferOperationRepository repository;

    @BeforeEach
    void cleanUp() {
        repository.deleteAll();
    }

    @Test
    void shouldSendNotificationEventToKafka() {
        // Given
        TransferOperation op = TransferOperation.builder()
                .operationId(repository.getNextOperationId())
                .username("alice")
                .recipient("bob")
                .amount(new BigDecimal("250.00"))
                .createdAt(LocalDateTime.now())
                .notificationAttemptsAt(LocalDateTime.now())
                .notificationAttempts(0)
                .build();
        repository.save(op);
        op.setStatus(OperationStatus.UPDATED);
        repository.save(op);

        // When

        // Then
        var valueDeserializer = new JacksonJsonDeserializer<>(NotificationEvent.class);
        valueDeserializer.addTrustedPackages("com.mybank.*");

        try (var consumer = new DefaultKafkaConsumerFactory<>(
                KafkaTestUtils.consumerProps(embeddedKafkaBroker, "test-group", true),
                new StringDeserializer(),
                valueDeserializer
        ).createConsumer()) {

            consumer.subscribe(List.of("notifications"));
            var record = KafkaTestUtils.getSingleRecord(consumer, "notifications", Duration.ofSeconds(15));
            assertThat(record.value().service()).isEqualTo("transfer-service");
            assertThat(record.value().username()).isEqualTo("alice");
            assertThat(record.value().operationId()).isEqualTo(op.getOperationId());
        }

        // And: статус операции = NOTIFIED
        TransferOperation updated = repository.findById(op.getOperationId()).orElseThrow();
        assertThat(updated.getStatus()).isEqualTo(OperationStatus.NOTIFIED);
    }
}
