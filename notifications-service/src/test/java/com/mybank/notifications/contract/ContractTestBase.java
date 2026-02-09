package com.mybank.notifications.contract;


import com.mybank.notifications.config.TestSecurityItConfig;
import com.mybank.notifications.dto.NotificationRequest;
import com.mybank.notifications.service.NotificationService;
import com.mybank.notifications.service.OutboxProcessor;
import io.restassured.module.mockmvc.RestAssuredMockMvc;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TestSecurityItConfig.class)
public abstract class ContractTestBase {

    @Autowired
    protected MockMvc mockMvc;

    @MockitoBean
    NotificationService notificationService;

    @MockitoBean
    OutboxProcessor outboxProcessor;


    @BeforeEach
    void setup() {
        RestAssuredMockMvc.mockMvc(mockMvc);

        RestAssuredMockMvc.postProcessors(
                jwt().jwt(j -> j
                        .claim("azp", "some-service")
                        .claim("clientRoles", "notification.write")
                ).authorities(new SimpleGrantedAuthority("ROLE_notification.write"))
        );

        when(notificationService.createAndEnqueue(any(NotificationRequest.class),anyString()))
                .thenReturn(true);

    }
}
