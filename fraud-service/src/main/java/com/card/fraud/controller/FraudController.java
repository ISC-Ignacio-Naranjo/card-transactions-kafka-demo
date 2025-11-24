package com.card.fraud.controller;

import com.card.fraud.model.FraudDecision;
import com.card.fraud.service.FraudService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/api/v1/fraud")
public class FraudController {

    private final FraudService fraudService;

    public FraudController(FraudService fraudService) {
        this.fraudService = fraudService;
    }

    @GetMapping("/decisions")
    public List<FraudDecision> getAllDecisions() {
        return fraudService.getAllDecisions();
    }

    @GetMapping("/decisions/transaction/{transactionId}")
    public FraudDecision getByTransactionId(@PathVariable Long transactionId) {
        return fraudService.getByTransactionId(transactionId)
                .orElseThrow(() -> new ResponseStatusException(
                HttpStatus.NOT_FOUND,
                "Fraud decision not found for transactionid: " + transactionId
        ));
    }
}
