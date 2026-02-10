package contracts.transfer


import org.springframework.cloud.contract.spec.Contract

Contract.make {
    description("Perform cash operation (deposit/withdraw)")
    request {
        method POST()
        url "/transfer/transfer"
        headers { contentType(applicationJson()) }
        body(
                operationId: 12345,
                recipient: $(consumer(regex('.+')), producer("bob")),
                amount: $(consumer(regex('[0-9]+(\\.[0-9]{1,2})?')), producer("100.00"))
        )
    }
    response {
        status NO_CONTENT()
    }
}
