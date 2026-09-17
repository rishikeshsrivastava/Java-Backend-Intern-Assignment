package com.example.Java_Backend_Intern_Assignment.service;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.Java_Backend_Intern_Assignment.dto.TransactionRequest;
import com.example.Java_Backend_Intern_Assignment.dto.TransactionResponse;
import com.example.Java_Backend_Intern_Assignment.entity.Transaction;
import com.example.Java_Backend_Intern_Assignment.entity.Wallet;
import com.example.Java_Backend_Intern_Assignment.repository.TransactionRepository;
import com.example.Java_Backend_Intern_Assignment.repository.WalletRepository;

@Service
public class TransactionService {

    private final WalletRepository walletRepository;
    private final TransactionRepository transactionRepository;

    public TransactionService(WalletRepository walletRepository,
                              TransactionRepository transactionRepository) {
        this.walletRepository = walletRepository;
        this.transactionRepository = transactionRepository;
    }

    @Transactional
    public TransactionResponse processTransaction(TransactionRequest request) {

        Wallet wallet = walletRepository.findByUserIdForUpdate(request.userId())
        .orElseThrow(() -> new RuntimeException("Wallet not found"));

        Optional<Transaction> existingTransaction =
        transactionRepository.findByTransactionId(request.transactionId());

        if (existingTransaction.isPresent()) {

            Transaction transaction = existingTransaction.get();

            return new TransactionResponse(
                    transaction.getTransactionId(),
                    transaction.getUserId(),
                    transaction.getAmount(),
                    transaction.getType(),
                    transaction.getBalanceAfter(),
                    transaction.getCreatedAt()
            );
        }

        if (request.amount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Amount must be greater than zero");
        }

        if (request.type() == com.example.Java_Backend_Intern_Assignment.entity.TransactionType.DEBIT) {
            if (wallet.getBalance().compareTo(request.amount()) < 0) {
                throw new IllegalStateException("Insufficient wallet balance");
            }

            BigDecimal newBalance = wallet.getBalance().subtract(request.amount()).setScale(2);
            wallet.setBalance(newBalance);
            wallet = walletRepository.save(wallet);
        } else {
            throw new IllegalArgumentException("Unsupported transaction type: " + request.type());
        }

        Transaction transaction = new Transaction();
        transaction.setTransactionId(request.transactionId());
        transaction.setUserId(request.userId());
        transaction.setAmount(request.amount());
        transaction.setType(request.type());
        transaction.setBalanceAfter(wallet.getBalance());
        transaction.setCreatedAt(Instant.now());

        Transaction savedTransaction = transactionRepository.save(transaction);

        return new TransactionResponse(
                savedTransaction.getTransactionId(),
                savedTransaction.getUserId(),
                savedTransaction.getAmount(),
                savedTransaction.getType(),
                savedTransaction.getBalanceAfter(),
                savedTransaction.getCreatedAt()
        );
    }
}
