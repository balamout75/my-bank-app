package contracts.notifications

import org.springframework.cloud.contract.spec.Contract

Contract.make {
    description("Create notification (Outbox). Returns 202 Accepted.")
    request {
        method POST()
        url "/notifications"
        headers { contentType(applicationJson()) }
        body(
                operationId: $(consumer(regex('[0-9]+')), producer(12345)),
                username: $(consumer(regex('.+')), producer("alice")),
                payload: [
                        type: $(consumer(anyNonBlankString()), producer("BALANCE_UPDATED")),
                        amount: $(consumer(regex('[0-9]+(\\.[0-9]{1,2})?')), producer("100.00")),
                        currency: $(consumer(anyNonBlankString()), producer("RUB"))
                ]
        )
    }
    response {
        status ACCEPTED()
    }
}
