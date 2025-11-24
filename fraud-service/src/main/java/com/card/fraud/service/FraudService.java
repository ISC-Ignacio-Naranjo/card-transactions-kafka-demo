package com.card.fraud.service;

import com.card.fraud.event.TransactionCreatedEvent;
import com.card.fraud.model.FraudDecision;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Service
@Slf4j
public class FraudService {

    // Simple in-memory storage for demo purposes
    private final Map<Long, FraudDecision> decisionsByTransactionId = new ConcurrentHashMap<>();

    public FraudDecision evaluate(TransactionCreatedEvent event) {
        BigDecimal amount = event.amount();

        String fraudStatus;
        String riskLevel;
        String reason;

        if (amount.compareTo(new BigDecimal("50000")) >= 0) {
            fraudStatus = "REJECTED";
            riskLevel = "HIGH";
            reason = "Amount above hard limit 50,000";
        }else if (amount.compareTo(new BigDecimal("10000")) >= 0) {
            fraudStatus = "REVIEW";
            riskLevel = "MEDIUM";
            reason = "Amount above review threshold 10,000";
        } else {
            fraudStatus = "CLEAR";
            riskLevel = "LOW";
            reason = "No risk rules matched";
        }
        FraudDecision decision = new FraudDecision(
                event.id(),
                event.userId(),
                amount,
                event.status(),
                fraudStatus,
                riskLevel,
                reason,
                Instant.now()
        );

        decisionsByTransactionId.put(event.id(), decision);
        log.info("Fraud decision for transaction {}: status={}, risk={}, reason={}",
                event.id(), fraudStatus, riskLevel, reason);

        return decision;
    }

    public List<FraudDecision> getAllDecisions() {
        return new ArrayList<>(decisionsByTransactionId.values());
    }

    public Optional<FraudDecision> getByTransactionId(Long transactionId) {
        return Optional.ofNullable(decisionsByTransactionId.get(transactionId));
    }
}
