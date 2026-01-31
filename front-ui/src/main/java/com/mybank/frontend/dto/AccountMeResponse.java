package com.mybank.frontend.dto;

import java.time.LocalDate;

public record AccountMeResponse(
        String username,
        String firstName,
        String lastName,
        LocalDate dateOfBirth,
        Integer balance
) {}
