package com.card.transactions.dto;

import java.math.BigDecimal;
import java.time.Instant;

public record TransactionResponse (
        Long id,
        String userId,
        BigDecimal amount,
        String status,
        Instant createdAt
) { }
