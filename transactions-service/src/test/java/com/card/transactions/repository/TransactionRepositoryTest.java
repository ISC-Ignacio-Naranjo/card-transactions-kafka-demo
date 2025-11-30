package com.card.transactions.repository;


import com.card.transactions.model.TransactionEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers
@SpringBootTest
public class TransactionRepositoryTest {

    @Container
    static PostgreSQLContainer<?> postgres =
            new PostgreSQLContainer<>("postgres:16-alpine")
                    .withDatabaseName("transactions")
                    .withUsername("postgres")
                    .withPassword("postgres");
    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired
    private TransactionRepository transactionRepository;

    @BeforeEach
    void cleanDb() {
        transactionRepository.deleteAll();
    }

    @Test
    void shouldPersistAndLoadTransactionEntity(){

        // Given
        TransactionEntity tx = new TransactionEntity();

         tx.setUserId("integration-user");
         tx.setAmount(new BigDecimal("1234.56"));
         tx.setStatus("CREATED");
         tx.setCreatedAt(Instant.now());

         TransactionEntity saved = transactionRepository.save(tx);

         // when
        var foundOpt = transactionRepository.findById(saved.getId());

        // Then
        assertThat(foundOpt).isPresent();
        TransactionEntity found = foundOpt.get();

        assertThat(found.getUserId()).isEqualTo("integration-user");
        assertThat(found.getAmount()).isEqualByComparingTo("1234.56");
        assertThat(found.getStatus()).isEqualTo("CREATED");

    }

}
