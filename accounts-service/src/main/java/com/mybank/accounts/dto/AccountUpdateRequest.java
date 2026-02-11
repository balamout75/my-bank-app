package com.mybank.accounts.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;

import java.time.LocalDate;

public record AccountUpdateRequest(
        Long operationId,
        @NotBlank String firstName,
        @NotBlank String lastName,
        @NotNull @Past LocalDate dateOfBirth
) {}
