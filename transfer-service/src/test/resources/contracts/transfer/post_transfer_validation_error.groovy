package contracts.transfer

import org.springframework.cloud.contract.spec.Contract

Contract.make {
    description("Reject cash operation when amount < 0.01 (validation)")
    request {
        method POST()
        url "/transfer/transfer"
        headers { contentType(applicationJson()) }
        body(
                operationId: 12345,
                recipient: $(consumer(regex('.+')), producer("bob")),
                amount: "0.00"
        )
    }
    response {
        status BAD_REQUEST()
    }
}
