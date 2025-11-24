package com.card.fraud.listener;

import com.card.fraud.event.TransactionCreatedEvent;
import com.card.fraud.model.FraudDecision;
import com.card.fraud.service.FraudService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class FraudListener {

    private final FraudService fraudService;

    public FraudListener(FraudService fraudService) {
        this.fraudService = fraudService;
    }

    @KafkaListener(
            topics = "${app.topics.transaction-created}",
            containerFactory = "transactionCreatedKafkaListenerContainerFactory"
    )
    public void onTransactionCreated(TransactionCreatedEvent event) {
        log.info("Received TransactionCreatedEvent in fraud-service: {}", event);
        FraudDecision decision = fraudService.evaluate(event);

        log.info("final fraud decision: {}", decision);
    }
}
