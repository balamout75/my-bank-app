package com.mybank.frontend.dto.client;

import java.time.LocalDate;

public record AccountDto(
        String username,
        String firstName,
        String lastName,
        LocalDate dateOfBirth,
        Integer balance
) {}