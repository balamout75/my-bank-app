package com.mybank.frontend.consumer;

import com.mybank.frontend.dto.client.OperationKeyResponse;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestClient;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class TransferClientConsumerTest extends BaseClientConsumerTest {

    @Test
    void should_get_operation_key() {
        String baseUrl = stubFinder.findStubUrl("com.mybank", "transfer-service").toString();
        RestClient client = RestClient.builder().baseUrl(baseUrl).build();

        OperationKeyResponse response = client.get()
                .uri("/transfer/operation-key")
                .retrieve()
                .body(OperationKeyResponse.class);

        assertThat(response).isNotNull();
        assertThat(response.operationId()).isNotNull();
    }

    @Test
    void should_accept_transfer() {
        String baseUrl = stubFinder.findStubUrl("com.mybank", "transfer-service").toString();
        RestClient client = RestClient.builder().baseUrl(baseUrl).build();

        Map<String, Object> req = Map.of(
                "operationId", 12345,
                "recipient", "bob",
                "amount", "100.00"
        );

        ResponseEntity<Void> resp = client.post()
                .uri("/transfer/transfer")
                .header("Content-Type", "application/json")
                .body(req)
                .retrieve()
                .toBodilessEntity();

        assertThat(resp.getStatusCode().value()).isEqualTo(204);
    }
}
