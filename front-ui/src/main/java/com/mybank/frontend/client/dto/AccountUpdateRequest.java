package com.mybank.frontend.client.dto;

import java.time.LocalDate;

public record AccountUpdateRequest(
        String firstName,
        String lastName,
        LocalDate dateOfBirth
) {}
