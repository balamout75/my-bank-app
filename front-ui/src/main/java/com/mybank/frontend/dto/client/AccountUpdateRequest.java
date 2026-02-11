package com.mybank.frontend.dto.client;

import java.time.LocalDate;

public record AccountUpdateRequest(
        String firstName,
        String lastName,
        LocalDate dateOfBirth
) {}
