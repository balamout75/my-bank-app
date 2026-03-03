package contracts.accounts

import org.springframework.cloud.contract.spec.Contract

Contract.make {
    description("Update current user account info")
    request {
        method PUT()
        url "/accounts/me"
        headers { contentType(applicationJson()) }
        body(
                firstName: $(consumer(regex('.+')), producer("Alice")),
                lastName: $(consumer(regex('.+')), producer("Johnson")),
                dateOfBirth: $(consumer(regex('[0-9]{4}-[0-9]{2}-[0-9]{2}')), producer("1990-01-01"))
        )
    }
    response {
        status NO_CONTENT()
    }
}
