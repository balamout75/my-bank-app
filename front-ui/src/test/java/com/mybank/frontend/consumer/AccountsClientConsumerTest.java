package com.mybank.frontend.consumer;

import com.mybank.frontend.dto.client.AccountMeResponse;
import com.mybank.frontend.dto.client.AccountSummaryResponse;
import org.junit.jupiter.api.Test;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class AccountsClientConsumerTest extends BaseClientConsumerTest {

    @Test
    void should_get_me() {
        String baseUrl = stubFinder.findStubUrl("com.mybank", "accounts-service").toString();
        RestClient client = RestClient.builder().baseUrl(baseUrl).build();

        AccountMeResponse response = client.get()
                .uri("/accounts/me")
                .retrieve()
                .body(AccountMeResponse.class);

        assertThat(response).isNotNull();
        assertThat(response.username()).isEqualTo("alice");
        assertThat(response.balance()).isNotNull();
    }

    @Test
    void should_get_all_accounts() {
        String baseUrl = stubFinder.findStubUrl("com.mybank", "accounts-service").toString();
        RestClient client = RestClient.builder().baseUrl(baseUrl).build();

        List<AccountSummaryResponse> response = client.get()
                .uri("/accounts/all")
                .retrieve()
                .body(new ParameterizedTypeReference<>() {});

        assertThat(response).isNotEmpty();
        assertThat(response.getFirst().username()).isEqualTo("bob");
    }

    @Test
    void should_update_me() {
        String baseUrl = stubFinder.findStubUrl("com.mybank", "accounts-service").toString();
        RestClient client = RestClient.builder().baseUrl(baseUrl).build();

        Map<String, Object> req = Map.of(
                "firstName", "Alice",
                "lastName", "Johnson",
                "dateOfBirth", "1990-01-01"
        );

        ResponseEntity<Void> resp = client.put()
                .uri("/accounts/me")
                .header("Content-Type", "application/json")
                .body(req)
                .retrieve()
                .toBodilessEntity();

        assertThat(resp.getStatusCode().value()).isEqualTo(204);
    }
}
