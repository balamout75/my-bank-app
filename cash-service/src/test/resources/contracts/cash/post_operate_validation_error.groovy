package contracts.cash

import org.springframework.cloud.contract.spec.Contract

Contract.make {
    description("Reject cash operation when amount < 0.01 (validation)")
    request {
        method POST()
        url "/cash/operate"
        headers { contentType(applicationJson()) }
        body(
                operationId: 12345,
                cashOperationType: "DEPOSIT",
                amount: "0.00"
        )
    }
    response {
        status BAD_REQUEST()
    }
}
