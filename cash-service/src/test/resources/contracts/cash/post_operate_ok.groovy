package contracts.cash

import org.springframework.cloud.contract.spec.Contract

Contract.make {
    description("Perform cash operation (deposit/withdraw)")
    request {
        method POST()
        url "/cash/operate"
        headers { contentType(applicationJson()) }
        body(
                operationId: 12345,
                cashOperationType: $(consumer(anyOf("DEPOSIT","WITHDRAW")), producer("DEPOSIT")),
                amount: $(consumer(regex('[0-9]+(\\.[0-9]{1,2})?')), producer("100.00"))
        )
    }
    response {
        status NO_CONTENT()
    }
}
