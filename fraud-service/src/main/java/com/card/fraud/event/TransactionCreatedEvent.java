package com.card.fraud.event;

import java.math.BigDecimal;
import java.time.Instant;

public record TransactionCreatedEvent(
        Long id,
        String userId,
        BigDecimal amount,
        String status,
        Instant createdAt
) { }
