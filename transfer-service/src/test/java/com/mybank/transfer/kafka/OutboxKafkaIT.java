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
import java.util.stream.StreamSupport;

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

            NotificationEvent found = null;
            var deadline = System.currentTimeMillis() + 15_000;
            while (found == null && System.currentTimeMillis() < deadline) {
                var records = consumer.poll(Duration.ofMillis(500));
                found = StreamSupport.stream(records.records("notifications").spliterator(), false)
                        .map(r -> r.value())
                        .filter(e -> e.operationId().equals(op.getOperationId()))
                        .findFirst()
                        .orElse(null);
            }
            assertThat(found).as("NotificationEvent с opId=" + op.getOperationId()).isNotNull();
            assertThat(found.service()).isEqualTo("transfer-service");
            assertThat(found.username()).isEqualTo("alice");
        }

        // And: статус операции = NOTIFIED
        TransferOperation updated = repository.findById(op.getOperationId()).orElseThrow();
        assertThat(updated.getStatus()).isEqualTo(OperationStatus.NOTIFIED);
    }
}
