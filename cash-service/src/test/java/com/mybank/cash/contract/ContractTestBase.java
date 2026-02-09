package com.mybank.cash.contract;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc(addFilters = false)
public abstract class ContractTestBase {
    @MockitoBean
    ClientRegistrationRepository clientRegistrationRepository;
    @MockitoBean
    OAuth2AuthorizedClientService oAuth2AuthorizedClientService;

}