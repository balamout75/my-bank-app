package com.mybank.frontend.dto.client;

import lombok.*;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TransferDto {
    private String toUsername;
    private BigDecimal amount;
}