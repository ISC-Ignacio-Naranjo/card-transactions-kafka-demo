package com.card.transactions.service;

import com.card.transactions.dto.CreateTransactionRequest;
import com.card.transactions.dto.TransactionResponse;
import com.card.transactions.event.TransactionCreatedEvent;
import com.card.transactions.exception.TransactionNotFoundException;
import com.card.transactions.model.TransactionEntity;
import com.card.transactions.repository.TransactionRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;

@Service
public class TransactionService {

    private final TransactionRepository repository;
    private final KafkaTemplate<String, TransactionCreatedEvent> kafkaTemplate;
    private final String transactionCreatedTopic;

    public TransactionService(TransactionRepository repository,
                              KafkaTemplate<String, TransactionCreatedEvent> kafkaTemplate,
                              @Value("${app.topics.transaction-created}") String transactionCreatedTopic) {
        this.repository = repository;
        this.kafkaTemplate = kafkaTemplate;
        this.transactionCreatedTopic = transactionCreatedTopic;
    }

    public TransactionResponse createTransaction(CreateTransactionRequest request) {
        TransactionEntity entity = TransactionEntity.builder()
                .userId(request.userId())
                .amount(request.amount())
                .status("CREATED")
                .createdAt(Instant.now())
                .build();

        TransactionEntity saved = repository.save(entity);

        // Build event to be published to Kafka
        TransactionCreatedEvent event = new TransactionCreatedEvent(
                saved.getId(),
                saved.getUserId(),
                saved.getAmount(),
                saved.getStatus(),
                saved.getCreatedAt()
        );

        // Use userId as key so messages for the same user go to the same partition
        kafkaTemplate.send(transactionCreatedTopic, saved.getUserId(), event);

        return toResponse(saved);
    }

    public TransactionResponse getById(Long id) {
        TransactionEntity entity = repository.findById(id)
                .orElseThrow(() -> new TransactionNotFoundException(id));
        return toResponse(entity);
    }

    public List<TransactionResponse> getByUserId(String userId) {
        return repository.findByUserId(userId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    public List<TransactionResponse> getAll() {
        return repository.findAll()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    private TransactionResponse toResponse(TransactionEntity entity) {
        return new TransactionResponse(
                entity.getId(),
                entity.getUserId(),
                entity.getAmount(),
                entity.getStatus(),
                entity.getCreatedAt()
        );
    }
}
