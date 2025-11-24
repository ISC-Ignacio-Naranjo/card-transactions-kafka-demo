package com.card.transactions.event;

import java.math.BigDecimal;
import java.time.Instant;

public record TransactionCreatedEvent(
        Long id,
        String userId,
        BigDecimal amount,
        String status,
        Instant createdAt
) { }
