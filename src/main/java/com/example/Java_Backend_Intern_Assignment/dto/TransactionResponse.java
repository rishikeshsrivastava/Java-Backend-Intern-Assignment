package com.example.Java_Backend_Intern_Assignment.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import com.example.Java_Backend_Intern_Assignment.entity.TransactionType;

public record TransactionResponse(    
    UUID transactionId,
    UUID userId,
    BigDecimal amount,
    TransactionType type,
    BigDecimal balanceAfter,
    Instant processedAt) {}
