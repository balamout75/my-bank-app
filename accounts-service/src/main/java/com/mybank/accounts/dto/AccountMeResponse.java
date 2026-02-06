package com.mybank.accounts.dto;

import lombok.Builder;

import java.math.BigDecimal;
import java.time.LocalDate;

@Builder
public record AccountMeResponse(
        String username,
        String firstName,
        String lastName,
        LocalDate dateOfBirth,
        BigDecimal balance
) {}
