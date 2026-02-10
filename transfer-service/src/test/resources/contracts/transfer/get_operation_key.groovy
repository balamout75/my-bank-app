package contracts.transfer

import org.springframework.cloud.contract.spec.Contract

import static org.springframework.cloud.contract.spec.internal.MediaTypes.APPLICATION_JSON

Contract.make {
    description("Generate idempotency key for cash operation")
    request {
        method GET()
        url "/transfer/operation-key"
    }
    response {
        status OK()
        headers {
            contentType(APPLICATION_JSON)
        }
        body(
                operationId: $(consumer(anyNumber()), producer(1L))
        )
    }
}