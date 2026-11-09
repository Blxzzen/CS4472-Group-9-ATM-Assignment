# Assignment 1 · Black box testing

Group 9 · Saif, Devon, and Thierry

## Process and assumptions

| Target | Assignment section | Technique and design |
|---|---|---|
| Session PIN and withdrawal choice 1 | ATM Session and The Testing Targets | [Robust boundary analysis under the single-fault assumption](session-testing.md#process-and-assumptions) |
| Withdrawal fee | Withdrawal | [Robust worst-case boundary value analysis](withdrawal-bva.md#process-and-assumptions) |
| Deposit fee | Deposit | [Weak robust equivalence class analysis](thierry-deposit-transfer-review.md#deposit) |
| Transfer fee | Transfer | [Decision table analysis](thierry-deposit-transfer-review.md#transfer) |

The Swing starter has no configurable daily limit. The Session tests use a fresh Chequing account fixture with a 5,000-dollar available balance and a 1,000-dollar specification boundary. The over-limit findings are limited to that setup. For fee inputs outside the documented domain, the tests record observed behavior and report no pass/fail verdict.

## Cases and results

The Java 11 and Maven 3.9.9 clean run produced the results below. Checkstyle reported zero violations. A Surefire skip in these new suites is a robustness observation where the calculator was called and the specification gave no expected outcome.

| Suite | Per-case inputs and results | Ran | Passed | Failed | Observations | Errors |
|---|---|---:|---:|---:|---:|---:|
| `SessionTest` | [PIN, withdrawal, and supplemental ATM PIN](session-testing.md#test-cases-and-observed-results) | 55 | 33 | 22 | 0 | 0 |
| `WithdrawalFeeTest` | [Complete boundary grid](withdrawal-bva.md#complete-case-results) | 1512 | 329 | 595 | 588 | 0 |
| `DepositFeeTest` | [Equivalence classes and boundary probes](thierry-deposit-transfer-review.md#deposit-results) | 24 | 4 | 16 | 4 | 0 |
| `TransferFeeTest` | [Decision rule and threshold probes](thierry-deposit-transfer-review.md#transfer-results) | 41 | 0 | 41 | 0 | 0 |
| Starter tests | `ATMTest` and `FeesCalculatorTest` | 3 | 3 | 0 | 0 | 0 |
| **Total** | | **1635** | **369** | **674** | **592** | **0** |

Fault locations and case IDs appear in the [Session](session-testing.md#fault-localization), [withdrawal](withdrawal-bva.md#fault-localization), and [deposit and transfer](thierry-deposit-transfer-review.md#fault-localization) tables. The [Session](session-failure-log.csv), [withdrawal](withdrawal-failure-log.csv), and [deposit and transfer](deposit-transfer-failure-log.csv) logs retain the timing evidence for Assignment 3.

```bash
mvn -B checkstyle:check
mvn -B clean -Dmaven.test.failure.ignore=true test
```

AI assistance was used for Session testing and final integration and verification.
