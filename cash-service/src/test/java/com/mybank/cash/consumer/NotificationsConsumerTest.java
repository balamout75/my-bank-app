package com.mybank.cash.consumer;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.contract.stubrunner.StubFinder;
import org.springframework.cloud.contract.stubrunner.spring.AutoConfigureStubRunner;
import org.springframework.cloud.contract.stubrunner.spring.StubRunnerProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.web.client.RestClient;
import org.springframework.http.ResponseEntity;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = NotificationsConsumerTest.TestConfig.class)
@AutoConfigureStubRunner(
        ids = "com.mybank:notifications-service:+:stubs",
        stubsMode = StubRunnerProperties.StubsMode.LOCAL
)
class NotificationsConsumerTest {

    @Configuration
    static class TestConfig {
        // пусто — нам не нужен Boot, только Spring TestContext + Stub Runner
    }

    @Autowired
    StubFinder stubFinder;

    @Test
    void should_accept_notification_request() {
        String baseUrl = stubFinder
                .findStubUrl("com.mybank", "notifications-service")
                .toString();

        RestClient client = RestClient.builder()
                .baseUrl(baseUrl)
                .defaultHeader("Content-Type", "application/json")
                .build();

        Map<String, Object> req = Map.of(
                "operationId", 12345,
                "username", "alice",
                "payload", Map.of(
                        "operation", "BALANCE_UPDATED",
                        "amount", "100.00"
                )
        );

        ResponseEntity<Void> resp = client.post()
                .uri("/notifications")
                .body(req)
                .retrieve()
                .toBodilessEntity();

        assertThat(resp.getStatusCode().value()).isEqualTo(202);
    }
}
