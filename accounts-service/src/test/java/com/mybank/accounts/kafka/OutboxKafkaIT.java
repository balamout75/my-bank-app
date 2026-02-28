package com.mybank.accounts.kafka;

import com.mybank.accounts.config.TestSecurityItConfig;
import com.mybank.accounts.dto.NotificationEvent;
import com.mybank.accounts.model.AccountOperation;
import com.mybank.accounts.model.OperationStatus;
import com.mybank.accounts.repository.AccountOperationRepository;
import com.mybank.accounts.template.BaseIntegrationTest;
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

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
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
    AccountOperationRepository repository;

    @Test
    void shouldSendNotificationEventToKafka() {
        // Given
        AccountOperation op = new AccountOperation();
        op.setOperationId(repository.nextOperationId());
        op.setUsername("alice");
        op.setPayload(Map.of("firstName", "Alice", "lastName", "Smith"));
        op.setStatus(OperationStatus.UPDATED);
        op.setAttempts(0);
        op.setCreatedAt(LocalDateTime.now());
        op.setUpdatedAt(LocalDateTime.now());
        repository.save(op);

        // When — scheduler отправит сам

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
            assertThat(found.service()).isEqualTo("accounts-service");
            assertThat(found.username()).isEqualTo("alice");
        }
        // And: статус операции = NOTIFIED
        AccountOperation updated = repository.findById(op.getOperationId()).orElseThrow();
        assertThat(updated.getStatus()).isEqualTo(OperationStatus.NOTIFIED);
    }
}
