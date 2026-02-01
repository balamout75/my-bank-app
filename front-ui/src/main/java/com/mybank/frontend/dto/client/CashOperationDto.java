package com.mybank.frontend.dto.client;

import lombok.*;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CashOperationDto {
    private BigDecimal amount;
}