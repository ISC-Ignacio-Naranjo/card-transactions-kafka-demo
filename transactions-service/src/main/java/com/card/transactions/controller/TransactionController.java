package com.card.transactions.controller;

import com.card.transactions.dto.CreateTransactionRequest;
import com.card.transactions.dto.TransactionResponse;
import com.card.transactions.service.TransactionService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/transactions")
public class TransactionController {

    private  final TransactionService service;

    public TransactionController(TransactionService service) {
        this.service = service;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public TransactionResponse create(@Valid @RequestBody CreateTransactionRequest request) {
        return service.createTransaction(request);
    }

    @GetMapping("/{id}")
    public TransactionResponse getById(@PathVariable Long id) {
        return service.getById(id);
    }

    @GetMapping("/user/{userId}")
    public List<TransactionResponse> getByUserId(@PathVariable String userId) {
        return service.getByUserId(userId);
    }

    @GetMapping
    public List<TransactionResponse> getAll() {
        return service.getAll();
    }
}
