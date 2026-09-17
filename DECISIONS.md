# Project Decisions

This document captures the key decisions made for this wallet transaction project based on the current implementation and test coverage.

## 1. Technology stack

- Java 21 is used as the project runtime.
- Spring Boot 4.1.1 is used as the application framework.
- Maven is used for build, packaging, and dependency management.
- H2 in-memory database is used for persistence during development and testing.
- Spring Data JPA is used for repository access and persistence logic.

## 2. Application architecture

- The project follows a layered Spring Boot structure:
  - controller layer for HTTP endpoints
  - service layer for business logic
  - repository layer for database access
  - entity layer for persistence models
  - DTO layer for request/response contracts
- The service layer owns the validation and state-changing logic for wallet transactions.

## 3. Wallet domain decisions

- Each wallet is associated with a unique `userId`.
- A wallet has a `balance` and `version` field.
- `@Version` is included on the wallet entity to support optimistic locking and safer concurrent updates.
- The repository exposes `findByUserId` and `findByUserIdForUpdate` so the service can fetch a wallet and lock it for updates when needed.
- The wallet balance is treated as money, so `BigDecimal` is used instead of floating-point types.

## 4. Monetary value format

- Money values are stored and compared using `BigDecimal`.
- The project standardizes money to 2 decimal places for wallet balances and transaction amounts.
- This decision avoids floating-point precision problems and keeps the business behavior consistent with financial-style values.

## 5. Transaction behavior

- A transaction is identified by a unique `transactionId`.
- A transaction belongs to a user and stores amount, type, balance after update, and created timestamp.
- The supported transaction type is `DEBIT`.
- Credit transactions are intentionally not supported in the current implementation.
- A transaction request is validated with `@Valid`, including non-null checks and minimum amount validation.

## 6. Duplicate protection strategy

- Duplicate transaction IDs are prevented by checking whether a transaction already exists before processing it.
- If the same transaction ID is submitted again, the service returns the previously saved transaction instead of re-processing it.
- This decision ensures idempotency for repeated requests carrying the same transaction ID.

## 7. Concurrent transaction safety

- The wallet is locked during the debit flow to avoid race conditions when multiple threads try to debit the same wallet at once.
- The service uses a transactional method so the wallet update and transaction persistence happen together as one unit of work.
- When a wallet does not have enough balance, the service throws `IllegalStateException` with the message `Insufficient wallet balance`.

## 8. API contract

- The endpoint is exposed as:
  - `POST /api/v1/transactions/process`
- The controller accepts a `TransactionRequest` and returns a `TransactionResponse`.
- The response includes the transaction ID, user ID, amount, type, wallet balance after processing, and timestamp.

## 9. Test strategy

The project includes integration-style tests validating real Spring Boot behavior.

- Single debit scenario verifies the wallet balance decreases correctly.
- Concurrent duplicate request scenario ensures identical transaction IDs are processed only once.
- Ten concurrent debit scenario ensures the final balance is correct and insufficient-funds failures match the expected count.

## 10. Results and status

- The project builds successfully with Maven using Java 21.
- The current implementation favors correctness, concurrency safety, and idempotent transaction processing over broad feature expansion.
