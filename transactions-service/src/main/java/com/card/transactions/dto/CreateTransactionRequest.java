package com.card.transactions.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record CreateTransactionRequest(
        @NotBlank
        String userId,

        @NotNull
        @DecimalMin(value = "0.01")
        BigDecimal amount
) { }
