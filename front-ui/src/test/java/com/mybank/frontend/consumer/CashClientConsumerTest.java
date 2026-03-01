package com.mybank.frontend.consumer;

import com.mybank.frontend.dto.client.CashOperationType;
import com.mybank.frontend.dto.client.OperationKeyResponse;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestClient;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class CashClientConsumerTest extends BaseClientConsumerTest {

    @Test
    void should_get_operation_key() {
        String baseUrl = stubFinder.findStubUrl("com.mybank", "cash-service").toString();
        RestClient client = RestClient.builder().baseUrl(baseUrl).build();

        OperationKeyResponse response = client.get()
                .uri("/cash/operation-key")
                .retrieve()
                .body(OperationKeyResponse.class);

        assertThat(response).isNotNull();
        assertThat(response.operationId()).isNotNull();
    }

    @Test
    void should_accept_deposit_operation() {
        String baseUrl = stubFinder.findStubUrl("com.mybank", "cash-service").toString();
        RestClient client = RestClient.builder().baseUrl(baseUrl).build();

        Map<String, Object> req = Map.of(
                "operationId", 12345,
                "cashOperationType", CashOperationType.DEPOSIT.name(),
                "amount", "100.00"
        );

        ResponseEntity<Void> resp = client.post()
                .uri("/cash/operate")
                .header("Content-Type", "application/json")
                .body(req)
                .retrieve()
                .toBodilessEntity();

        assertThat(resp.getStatusCode().value()).isEqualTo(204);
    }
}
