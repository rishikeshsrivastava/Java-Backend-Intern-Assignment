package com.example.Java_Backend_Intern_Assignment.dto;

import java.math.BigDecimal;
import java.util.UUID;

import com.example.Java_Backend_Intern_Assignment.entity.TransactionType;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;

public record TransactionRequest(    
    @NotNull(message = "transactionId must not be null")
    UUID transactionId,

    @NotNull(message = "userId must not be null")
    UUID userId,

    @NotNull(message = "amount must not be null")
    @DecimalMin(value = "0.01", message = "amount must be greater than zero")
    @Digits(integer = 15, fraction = 4, message = "amount must have at most 15 integer digits and 4 decimal places")
    BigDecimal amount,

    @NotNull(message = "type must not be null")
    TransactionType type) {}
