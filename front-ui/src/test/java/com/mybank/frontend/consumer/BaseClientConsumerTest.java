package com.mybank.frontend.consumer;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.cloud.contract.stubrunner.StubFinder;
import org.springframework.cloud.contract.stubrunner.spring.AutoConfigureStubRunner;
import org.springframework.cloud.contract.stubrunner.spring.StubRunnerProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.web.client.RestClient;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = BaseClientConsumerTest.TestConfig.class)
@ActiveProfiles("consumer-test")
@ImportAutoConfiguration(exclude = {
        org.springframework.cloud.contract.stubrunner.spring.cloud.StubRunnerSpringCloudAutoConfiguration.class
})
@AutoConfigureStubRunner(
        stubsMode = StubRunnerProperties.StubsMode.LOCAL,
        ids = {
                "com.mybank:accounts-service:+:stubs",
                "com.mybank:cash-service:+:stubs",
                "com.mybank:transfer-service:+:stubs"
        }
)
public abstract class BaseClientConsumerTest {

    @Configuration
    static class TestConfig {
        // пустой
    }

    @Autowired
    protected StubFinder stubFinder;

    protected RestClient clientFor(String artifactId) {
        String baseUrl = stubFinder.findStubUrl("com.mybank", artifactId).toString();
        return RestClient.builder()
                .baseUrl(baseUrl)
                .build();
    }
}