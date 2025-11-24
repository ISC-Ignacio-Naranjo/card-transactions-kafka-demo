package com.card.transactions.exception;

public class TransactionNotFoundException  extends RuntimeException {

    public TransactionNotFoundException(Long id) {
        super("Transaction not found for id: " + id);
    }
}
