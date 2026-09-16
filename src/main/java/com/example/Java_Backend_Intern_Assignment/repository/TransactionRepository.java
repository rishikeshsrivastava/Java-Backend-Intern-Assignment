package com.example.Java_Backend_Intern_Assignment.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.Java_Backend_Intern_Assignment.entity.Transaction;

@Repository 
public interface  TransactionRepository extends JpaRepository<Transaction, Long> {

    boolean existsByTransactionId(UUID transactionId);

    Optional<Transaction> findByTransactionId(UUID transactionId);

    long countByTransactionId(UUID transactionId);
}
