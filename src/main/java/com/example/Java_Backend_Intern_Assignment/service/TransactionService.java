package com.example.Java_Backend_Intern_Assignment.service;

import java.math.BigDecimal;
import java.time.Instant;

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

        // Change 1: TransactionRequest is a Java record, so the accessor is request.userId(), not getUserId().
        Wallet wallet = walletRepository.findByUserId(request.userId())
                .orElseThrow(() -> new RuntimeException("Wallet not found"));

        // Change 2: validate the amount before changing the wallet balance.
        // if (request.amount().compareTo(BigDecimal.ZERO) <= 0) {
        //     throw new IllegalArgumentException("Amount must be greater than zero");
        // }
        if (wallet.getBalance().compareTo(request.getAmount()) < 0) {
            throw new RuntimeException("Insufficient balance");
        }

        // Change 3: keep the logic consistent with the enum value currently supported by the app.
        // if (request.type() == com.example.Java_Backend_Intern_Assignment.entity.TransactionType.DEBIT) {
        //     if (wallet.getBalance().compareTo(request.amount()) < 0) {
        //         throw new IllegalStateException("Insufficient wallet balance");
        //     }

        //     // Change 4: subtract the amount and persist the updated wallet.
        //     wallet.setBalance(wallet.getBalance().subtract(request.amount()));
        //     wallet = walletRepository.save(wallet);
        // } else {
        //     throw new IllegalArgumentException("Unsupported transaction type: " + request.type());
        // }

        wallet.setBalance(
            wallet.getBalance().subtract(request.getAmount())
        );

        walletRepository.save(wallet);
        

        // Change 5: build the transaction entity using the request data and the new wallet balance.
        // Transaction transaction = new Transaction();
        // transaction.setTransactionId(request.transactionId());
        // transaction.setUserId(request.userId());
        // transaction.setAmount(request.amount());
        // transaction.setType(request.type());
        // transaction.setBalanceAfter(wallet.getBalance());
        // transaction.setCreatedAt(Instant.now());

        Transaction transaction = new Transaction();

        transaction.setTransactionId(request.getTransactionId());
        transaction.setUserId(request.getUserId());
        transaction.setAmount(request.getAmount());
        transaction.setType(request.getType());

        // Change 6: save the transaction record and return the response DTO expected by the controller.
        // Transaction savedTransaction = transactionRepository.save(transaction);

        // return new TransactionResponse(
        //         savedTransaction.getTransactionId(),
        //         savedTransaction.getUserId(),
        //         savedTransaction.getAmount(),
        //         savedTransaction.getType(),
        //         savedTransaction.getBalanceAfter(),
        //         savedTransaction.getCreatedAt()
        // );

        return transactionRepository.save(transaction);
    }
}