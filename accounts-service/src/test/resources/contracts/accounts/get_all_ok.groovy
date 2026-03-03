package contracts.accounts

import org.springframework.cloud.contract.spec.Contract

import static org.springframework.cloud.contract.spec.internal.MediaTypes.APPLICATION_JSON

Contract.make {
    description("Get all other accounts (excluding current user)")
    request {
        method GET()
        url "/accounts/all"
    }
    response {
        status OK()
        headers {
            contentType(APPLICATION_JSON)
        }
        body([
                [username: "bob", fullName: "Bob Brown"]
        ])
    }
}
