package contracts.accounts

import org.springframework.cloud.contract.spec.Contract

import static org.springframework.cloud.contract.spec.internal.MediaTypes.APPLICATION_JSON

Contract.make {
    description("Get current user account info")
    request {
        method GET()
        url "/accounts/me"
    }
    response {
        status OK()
        headers {
            contentType(APPLICATION_JSON)
        }
        body(
                username: "alice",
                firstName: "Alice",
                lastName: "Smith",
                dateOfBirth: "1990-01-01",
                balance: 10000.00
        )
    }
}
