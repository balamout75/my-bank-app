package com.mybank.accounts.config;

import org.springframework.web.client.RestClient;

public record NotificationsRestClient(RestClient client) {}
