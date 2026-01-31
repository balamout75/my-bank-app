package com.mybank.frontend.client.dto;

import java.time.LocalDate;

public record AccountMeResponse(
        String username,
        String firstName,
        String lastName,
        LocalDate dateOfBirth,
        Integer balance
) {}