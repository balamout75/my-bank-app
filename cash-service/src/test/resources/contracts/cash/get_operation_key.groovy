package contracts.cash

import org.springframework.cloud.contract.spec.Contract

Contract.make {
    description("Generate idempotency key for cash operation")
    request {
        method GET()
        url "/cash/operation-key"
    }
    response {
        status OK()
        headers { contentType(applicationJson()) }
        body(
                operationId: value(
                        producer: 1L,
                        consumer: anyNumber()
                )
        )
    }
}
