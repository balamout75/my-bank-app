package contracts.notifications

import org.springframework.cloud.contract.spec.Contract

Contract.make {
    description("Create notification (Outbox). Returns 202 Accepted. Payload can be any non-empty JSON object.")
    request {
        method POST()
        url "/notifications"
        headers { contentType(applicationJson()) }
        body(
                operationId: $(consumer(regex('[0-9]+')), producer(12345)),
                username: $(consumer(regex('.+')), producer("alice")),
                payload: [ sample: "x" ]
        )
        bodyMatchers {
            jsonPath('$.payload.*', byRegex('.+'))
        }
    }
    response {
        status ACCEPTED()
    }
}
