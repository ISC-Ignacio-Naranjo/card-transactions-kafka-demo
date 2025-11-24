package com.card.fraud.model;

import java.math.BigDecimal;
import java.time.Instant;

public record FraudDecision(
        Long transactionId,
        String userId,
        BigDecimal amount,
        String transactionStatus,
        String fraudStatus,
        String riskLevel,
        String reason,
        Instant evaluatedAt
) {
}
