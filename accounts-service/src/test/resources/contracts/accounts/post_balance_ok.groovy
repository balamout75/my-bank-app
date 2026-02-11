package contracts.accounts

import org.springframework.cloud.contract.spec.Contract

Contract.make {
    description("Apply balance update in Accounts Service (internal endpoint)")
    request {
        method POST()
        url "/accounts/balance"
        headers { contentType(applicationJson()) }
        body(
                username: $(consumer(regex('.+')), producer("alice")),
                amount: $(consumer(regex('-?[0-9]+(\\.[0-9]{1,2})?')), producer("100.00")),
                cashOperationType: $(consumer(anyOf("DEPOSIT","WITHDRAW")), producer("DEPOSIT")),
                operationId: $(consumer(regex('[0-9]+')), producer(12345))
        )
    }
    response {
        status NO_CONTENT()
    }
}
