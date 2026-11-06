# Assignment 1 — Session black-box testing

## Process and assumptions

Source of expected behavior: Assignment 1, **ATM Session** and **The Testing Targets**. The required target is `atm.session.Session` and withdrawal choice 1.

- Apply robust boundary value analysis under the single-fault assumption. Each fresh session varies PIN format or withdrawal amount, holding the other inputs nominal. PIN tests end at the PIN step. Withdrawal tests use a valid five-digit PIN directly through Session so a defect in the ATM PIN gate cannot block the amount cases.
- A valid PIN is five ASCII digits, with leading zeroes preserved. Probe lengths 4, 5 and 6, plus empty and one-digit inputs. Exercise digit endpoints 0 and 9, their immediate outside characters / and :, a letter, an embedded space and a non-ASCII digit. The ASCII interpretation is an assumption. The supplemental `A-P` cases exercise `ATM.checkPin` with authentication stubbed to succeed. They are counted separately from direct Session coverage.
- Valid withdrawal amounts are positive whole dollars up to \$1000 that equal `20x + 50y` for nonnegative integers `x` and `y`. Probe \$19/\$20/\$21, \$49/\$50/\$51, \$999/\$1000/\$1001, zero and negative values, and single- and mixed-denomination amounts. \$1010 and \$1020 are both dispensable above the limit. \$1001 is also invalid on denomination grounds. Expected outcomes come from the specification and are recorded in the input CSVs.
- The supplied starter is Swing. A mocked panel allows headless tests through the public Session API. The withdrawal fixture supplies a fresh \$5000 Chequing balance through a mocked DB and runs the real dispatcher and withdrawal flow. A zero-fee test double isolates validation from fee defects. The starter exposes no daily-limit setting in its schema or account API. \$1000 is therefore the specification boundary. The over-limit cases cannot verify an actually configured limit. TA confirmation of the intended starter or fixture is pending.

## Test cases and observed results

Java 11.0.29 and Maven 3.9.9. Checkstyle reported zero violations. The targeted run and the clean suite agreed on all 55 new invocations. Of these, 33 passed and 22 failed assertions. The clean suite included three passing starter tests for 58 total, with zero errors or skips. Direct Session cases account for 40 invocations and 16 failures. The supplemental ATM cases account for 15 invocations and six failures. These results precede integration of the fee suites.

`mvn -B checkstyle:check`

`mvn -B -Dtest=SessionTest -Dmaven.test.failure.ignore=true test`

`mvn -B clean -Dmaven.test.failure.ignore=true test`

Surefire labels parameterized invocations with their test method and 1-based index. The IDs below follow the CSV row order, excluding the header.

### Session PIN format

| Case ID | PIN | Expected | Observed | Result |
|---|---|---|---|---|
| S-P01 | `1234` | InvalidPinFormatException | No exception | Fail (S-F01) |
| S-P02 | `12345` | No exception | No exception | Pass |
| S-P03 | `123456` | InvalidPinFormatException | No exception | Fail (S-F01) |
| S-P04 | `empty` | InvalidPinFormatException | No exception | Fail (S-F01) |
| S-P05 | `1` | InvalidPinFormatException | No exception | Fail (S-F01) |
| S-P06 | `00000` | No exception | No exception | Pass |
| S-P07 | `99999` | No exception | No exception | Pass |
| S-P08 | `/2345` | InvalidPinFormatException | No exception | Fail (S-F01) |
| S-P09 | `:2345` | InvalidPinFormatException | No exception | Fail (S-F01) |
| S-P10 | `12a45` | InvalidPinFormatException | No exception | Fail (S-F01) |
| S-P11 | `12 45` | InvalidPinFormatException | No exception | Fail (S-F01) |
| S-P12 | `1234٥` | InvalidPinFormatException | No exception | Fail (S-F01) |
| S-P13 | `0000` | InvalidPinFormatException | No exception | Fail (S-F01) |
| S-P14 | `9999` | InvalidPinFormatException | No exception | Fail (S-F01) |
| S-P15 | `88888` | No exception | No exception | Pass |

### Session withdrawal amount

| Case ID | Amount | Expected | Observed | Result |
|---|---|---|---|---|
| S-W01 | $-1 | InvalidAmountException | InvalidAmountException | Pass |
| S-W02 | \$0 | InvalidAmountException | No exception | Fail (S-F03) |
| S-W03 | \$1 | InvalidAmountException | InvalidAmountException | Pass |
| S-W04 | \$19 | InvalidAmountException | InvalidAmountException | Pass |
| S-W05 | \$20 | No exception | No exception | Pass |
| S-W06 | \$21 | InvalidAmountException | InvalidAmountException | Pass |
| S-W07 | \$30 | InvalidAmountException | InvalidAmountException | Pass |
| S-W08 | \$40 | No exception | No exception | Pass |
| S-W09 | \$50 | No exception | No exception | Pass |
| S-W10 | \$60 | No exception | InvalidAmountException | Fail (S-F04) |
| S-W11 | \$70 | No exception | No exception | Pass |
| S-W12 | \$90 | No exception | No exception | Pass |
| S-W13 | \$100 | No exception | No exception | Pass |
| S-W14 | \$500 | No exception | No exception | Pass |
| S-W15 | \$979 | InvalidAmountException | InvalidAmountException | Pass |
| S-W16 | \$980 | No exception | InvalidAmountException | Fail (S-F04) |
| S-W17 | \$990 | No exception | No exception | Pass |
| S-W18 | \$999 | InvalidAmountException | InvalidAmountException | Pass |
| S-W19 | \$1000 | No exception | No exception | Pass |
| S-W20 | \$1001 | InvalidAmountException | InvalidAmountException | Pass |
| S-W21 | \$1020 | InvalidAmountException | No exception | Fail (S-F05) |
| S-W22 | $-20 | InvalidAmountException | No exception | Fail (S-F03) |
| S-W23 | \$49 | InvalidAmountException | InvalidAmountException | Pass |
| S-W24 | \$51 | InvalidAmountException | InvalidAmountException | Pass |
| S-W25 | \$1010 | InvalidAmountException | InvalidAmountException | Pass |

### Supplemental ATM PIN format

| Case ID | PIN | Expected | Observed | Result |
|---|---|---|---|---|
| A-P01 | `1234` | InvalidPinFormatException | No exception | Fail (S-F02) |
| A-P02 | `12345` | No exception | InvalidPinFormatException | Fail (S-F02) |
| A-P03 | `123456` | InvalidPinFormatException | InvalidPinFormatException | Pass |
| A-P04 | `empty` | InvalidPinFormatException | InvalidPinFormatException | Pass |
| A-P05 | `1` | InvalidPinFormatException | InvalidPinFormatException | Pass |
| A-P06 | `00000` | No exception | InvalidPinFormatException | Fail (S-F02) |
| A-P07 | `99999` | No exception | InvalidPinFormatException | Fail (S-F02) |
| A-P08 | `/2345` | InvalidPinFormatException | InvalidPinFormatException | Pass |
| A-P09 | `:2345` | InvalidPinFormatException | InvalidPinFormatException | Pass |
| A-P10 | `12a45` | InvalidPinFormatException | InvalidPinFormatException | Pass |
| A-P11 | `12 45` | InvalidPinFormatException | InvalidPinFormatException | Pass |
| A-P12 | `1234٥` | InvalidPinFormatException | InvalidPinFormatException | Pass |
| A-P13 | `0000` | InvalidPinFormatException | No exception | Fail (S-F02) |
| A-P14 | `9999` | InvalidPinFormatException | InvalidPinFormatException | Pass |
| A-P15 | `88888` | No exception | InvalidPinFormatException | Fail (S-F02) |

Passing invalid-input cases demonstrate the observed exception. Some overlap with other rejection rules. For example, \$1010 currently fails the denomination check even though \$50 + 48 × \$20 is dispensable. The \$1020 case can expose the absent limit because it passes the starter’s denomination check.

## Fault localization

| Failure ID | Failing cases | Suspected location | Justification |
|---|---|---|---|
| S-F01 | S-P01, S-P03, S-P04, S-P05, S-P08, S-P09, S-P10, S-P11, S-P12, S-P13, S-P14 | [`Session.addPin`](../src/main/java/atm/session/Session.java#L56) | Session stores the input and advances state without checking length or characters. |
| S-F02 | A-P01, A-P02, A-P06, A-P07, A-P13, A-P15 | [`FormatChecker.checkPinFormat`](../src/main/java/atm/utils/FormatChecker.java#L33) | The four-character length check accepts short digit-only PINs and rejects all five-digit PINs. |
| S-F03 | S-W02, S-W22 | [`CashValidator.validateWithdrawal`](../src/main/java/atm/utils/CashValidator.java#L23) | The remainder check accepts zero and negative multiples without requiring a positive amount. |
| S-F04 | S-W10, S-W16 | [`CashValidator.validateWithdrawal`](../src/main/java/atm/utils/CashValidator.java#L23) | Taking the remainder after using the maximum number of \$50 bills misses valid combinations with fewer \$50 bills. |
| S-F05 | S-W21 | [`Session.setAmount`](../src/main/java/atm/session/Session.java#L87) | The withdrawal path checks denominations and the bank checks the balance, so the dispensable \$1020 passes without a daily-limit check. This is a specification-gap observation with the available starter. |

`FormatChecker` also uses `>= '9'` in its digit condition. The five-digit `99999` case reaches the length failure first, so its result cannot independently establish a digit-boundary fault.

## Evidence and contribution record

The [Session failure log](session-failure-log.csv) records elapsed automated testing and the batch localization window for Assignment 3. The same timings apply to the batch and should be counted once. AI assistance drafted and revised the tests, ran validation, and prepared the report. Team members should record their own actual review and diagnosis time separately.
