package contracts.accounts

import org.springframework.cloud.contract.spec.Contract

Contract.make {
    description("Apply transfer cash in Accounts Service (internal endpoint)")
    request {
        method POST()
        url "/accounts/transfer"
        headers { contentType(applicationJson()) }
        body(
                operationId: $(consumer(regex('[0-9]+')), producer(12345)),
                username: $(consumer(regex('.+')), producer("alice")),
                recipient: $(consumer(regex('.+')), producer("bob")),
                amount: $(consumer(regex('-?[0-9]+(\\.[0-9]{1,2})?')), producer("100.01"))
        )
    }
    response {
        status NO_CONTENT()
    }
}