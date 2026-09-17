package com.example.Java_Backend_Intern_Assignment;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import com.example.Java_Backend_Intern_Assignment.dto.TransactionRequest;
import com.example.Java_Backend_Intern_Assignment.dto.TransactionResponse;
import com.example.Java_Backend_Intern_Assignment.entity.TransactionType;
import com.example.Java_Backend_Intern_Assignment.entity.Wallet;
import com.example.Java_Backend_Intern_Assignment.repository.TransactionRepository;
import com.example.Java_Backend_Intern_Assignment.repository.WalletRepository;
import com.example.Java_Backend_Intern_Assignment.service.TransactionService;

@SpringBootTest
class TransactionServiceTest {

    @Autowired
    private TransactionService transactionService;

    @Autowired
    private WalletRepository walletRepository;

    @Autowired
    private TransactionRepository transactionRepository;

    @Test
    @DisplayName("Processes a single valid debit transaction successfully.")
    void processSingleDebitSuccessfully() {

        // Arrange

        UUID userId = UUID.randomUUID();
        UUID transactionId = UUID.randomUUID();

        Wallet wallet = new Wallet();
        wallet.setUserId(userId);
        wallet.setBalance(new BigDecimal("1000.00"));

        walletRepository.save(wallet);

        TransactionRequest request = new TransactionRequest(
                transactionId,
                userId,
                new BigDecimal("100.00"),
                TransactionType.DEBIT
        );

        // Act

        TransactionResponse response =
                transactionService.processTransaction(request);

        // Assert

        Wallet updatedWallet =
                walletRepository.findByUserId(userId)
                        .orElseThrow();

        assertEquals(
                new BigDecimal("900.00"),
                updatedWallet.getBalance()
        );
    }

	@Test
	@DisplayName("Sends 3 identical transactionIDs simultaneously. Ensures the balance is only deducted once.")
	void processesThreeIdenticalTransactionsConcurrently() throws Exception {

		// Arrange

		UUID userId = UUID.randomUUID();
		UUID transactionId = UUID.randomUUID();

		Wallet wallet = new Wallet();
		wallet.setUserId(userId);
		wallet.setBalance(new BigDecimal("1000.00"));

		walletRepository.save(wallet);

		TransactionRequest request = new TransactionRequest(
				transactionId,
				userId,
				new BigDecimal("100.00"),
				TransactionType.DEBIT
		);

		ExecutorService executor = Executors.newFixedThreadPool(3);

		CountDownLatch startLatch = new CountDownLatch(1);

		List<Future<TransactionResponse>> futures = new ArrayList<>();

		// Start 3 concurrent requests

		for (int i = 0; i < 3; i++) {

			Future<TransactionResponse> future = executor.submit(() -> {

				startLatch.await();

				return transactionService.processTransaction(request);
			});

			futures.add(future);
		}

		// Release all 3 threads at the same time

		startLatch.countDown();

		// Wait for all requests to finish

		for (Future<TransactionResponse> future : futures) {
			future.get();
		}

		executor.shutdown();

		// Assert wallet balance

		Wallet updatedWallet =
				walletRepository.findByUserId(userId)
						.orElseThrow();

		assertEquals(
				new BigDecimal("900.00"),
				updatedWallet.getBalance()
		);

		// Assert only one transaction exists

		long transactionCount =
				transactionRepository.countByTransactionId(transactionId);

		assertEquals(1, transactionCount);
	}

	@Test
	@DisplayName("Sends 10 concurrent debit requests of ₹100 for a wallet with a ₹500 balance. Ensures the final balance is exactly ₹0 and 5 requests fail with insufficient funds.")
	void processesTenConcurrentDebits() throws Exception {

		// Arrange

		UUID userId = UUID.randomUUID();

		Wallet wallet = new Wallet();
		wallet.setUserId(userId);
		wallet.setBalance(new BigDecimal("500.00"));

		walletRepository.save(wallet);

		ExecutorService executor = Executors.newFixedThreadPool(10);

		CountDownLatch startLatch = new CountDownLatch(1);

		List<Future<TransactionResponse>> futures = new ArrayList<>();

		// Create 10 different transaction requests

		for (int i = 0; i < 10; i++) {

			TransactionRequest request = new TransactionRequest(
					UUID.randomUUID(),
					userId,
					new BigDecimal("100.00"),
					TransactionType.DEBIT
			);

			Future<TransactionResponse> future = executor.submit(() -> {

				// Wait until all 10 threads are ready
				startLatch.await();

				// Process the debit
				return transactionService.processTransaction(request);
			});

			futures.add(future);
		}

		// Release all 10 threads
		startLatch.countDown();

		// Count successful and failed requests

		int successfulRequests = 0;
		int insufficientFundsRequests = 0;

		for (Future<TransactionResponse> future : futures) {

			try {

				future.get();
				successfulRequests++;

			} catch (Exception e) {

				// The service throws an exception when balance is insufficient
				if (e.getCause() instanceof IllegalStateException
						&& e.getCause().getMessage().equals("Insufficient wallet balance")) {

					insufficientFundsRequests++;

				} else {
					throw e;
				}
			}
		}

		executor.shutdown();

		// Assert number of successful requests

		assertEquals(5, successfulRequests);

		// Assert number of insufficient-funds failures

		assertEquals(5, insufficientFundsRequests);

		// Check final wallet balance

		Wallet updatedWallet =
				walletRepository.findByUserId(userId)
						.orElseThrow();

		assertEquals(
				new BigDecimal("0.00"),
				updatedWallet.getBalance()
		);
	}

}