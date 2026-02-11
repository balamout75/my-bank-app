package com.mybank.transfer.consumer;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.contract.stubrunner.StubFinder;
import org.springframework.cloud.contract.stubrunner.spring.AutoConfigureStubRunner;
import org.springframework.cloud.contract.stubrunner.spring.StubRunnerProperties;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.web.client.RestClient;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = NotificationsConsumerTest.TestConfig.class)
@AutoConfigureStubRunner(
        ids = "com.mybank:accounts-service:+:stubs",
        stubsMode = StubRunnerProperties.StubsMode.LOCAL
)
class AccountsClientConsumerTest {

    @Autowired
    StubFinder stubFinder;

    @Test
    void should_accept_balance_update() {
        String baseUrl = stubFinder.findStubUrl("com.mybank", "accounts-service").toString();

        RestClient client = RestClient.builder()
                .baseUrl(baseUrl)
                .defaultHeader("Content-Type", "application/json")
                .defaultHeader("Authorization", "Bearer dummy") // если контракт это требует
                .build();

        Map<String, Object> req = Map.of(
                "operationId", 1001,
                "username", "alice",
                "recipient", "bob",
                "amount", 10.00
        );

        ResponseEntity<Void> resp = client.post()
                .uri("/accounts/transfer")
                .body(req)
                .retrieve()
                .toBodilessEntity();

        // по твоему контроллеру сейчас NO_CONTENT (204)
        assertThat(resp.getStatusCode().value()).isEqualTo(204);
    }
}
