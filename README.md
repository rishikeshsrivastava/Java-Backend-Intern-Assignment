# Idempotent Payment / Wallet Event Processor

A Spring Boot backend service that processes wallet debit transactions safely under
concurrent requests.

The project demonstrates:

- Idempotent transaction processing
- Database-level locking for concurrent wallet updates
- Prevention of negative wallet balances
- H2 in-memory database
- REST API using Spring Boot
- JUnit 5 concurrency and integration tests

## Tech Stack

- Java 17
- Spring Boot
- Spring Data JPA / Hibernate
- H2 In-Memory Database
- JUnit 5
- Maven

---

## Project Overview

The service acts as an internal transaction ledger.

Payment gateways can send the same webhook multiple times due to network
retries. These duplicate requests must not deduct money from the wallet more
than once.

The service therefore uses the transaction ID as an idempotency key.

For concurrent debit requests on the same wallet, database-level pessimistic
locking is used to ensure that balance updates happen safely and the wallet
cannot become negative.

### Architecture

     Client
        |
        v
TransactionController
        |
        v
TransactionService
        |
+----------------------+
|                      |
v                      v
WalletRepository    TransactionRepository
|                      |
+----------+-----------+
            |
            v
    H2 In-Memory DB

## Sample Wallets

The application automatically creates sample wallets when it starts.

| User ID                                | Initial Balance |
|----------------------------------------|-----------------|
| `11111111-1111-1111-1111-111111111111` | ₹1000.00        |
| `22222222-2222-2222-2222-222222222222` | ₹500.00         |
| `33333333-3333-3333-3333-333333333333` | ₹250.00         |

These wallets are provided so the API can be tested immediately after starting the application.

## API

### Process Transaction

**Endpoint**

```
POST /api/v1/transactions/process
```

**Request Body**

```json
{
  "transactionId": "aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa",
  "userId": "11111111-1111-1111-1111-111111111111",
  "amount": 100.00,
  "type": "DEBIT"
}
```

**Expected Result**

The wallet initially has:

```
₹1000.00
```

After the ₹100 debit:

```
₹900.00
```

The response contains the transaction details and the wallet balance after processing.

Example response:

```json
{
  "transactionId": "aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa",
  "userId": "11111111-1111-1111-1111-111111111111",
  "amount": 100.00,
  "type": "DEBIT",
  "balanceAfter": 900.0000,
  "processedAt": "<generated timestamp>"
}
```

### Testing Idempotency

The same transaction can be sent multiple times using the same `transactionId`.

Example request:

```json
{
  "transactionId": "aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa",
  "userId": "11111111-1111-1111-1111-111111111111",
  "amount": 100.00,
  "type": "DEBIT"
}
```

The `transactionId` acts as the idempotency key.

If the same transaction is received again, the previously processed transaction is returned instead of deducting the wallet again.

Therefore:

```
Initial balance:          ₹1000
First request:            -₹100
Duplicate request:        No additional deduction
Final balance:            ₹900
```

This ensures that duplicate webhook requests do not result in duplicate wallet deductions.

### Testing Concurrent Debits

The ₹500 wallet can be used to understand and test the concurrency scenario.

**User ID:** `22222222-2222-2222-2222-222222222222`
**Initial Balance:** ₹500

If 10 concurrent requests of ₹100 are processed:

```
10 × ₹100 = ₹1000 requested
Available balance = ₹500
```

Only 5 requests can successfully debit the wallet.

**Expected Result**

```
Successful requests:       5
Insufficient funds:        5
Final wallet balance:      ₹0
```

Database-level locking ensures that concurrent requests cannot incorrectly read and update the same wallet balance at the same time.