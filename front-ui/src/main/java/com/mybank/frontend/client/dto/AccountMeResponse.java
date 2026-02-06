package com.mybank.frontend.client.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record AccountMeResponse(
        String username,
        String firstName,
        String lastName,
        LocalDate dateOfBirth,
        BigDecimal balance
) {}