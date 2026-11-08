# Deposit and transfer fees

## Process and assumptions

Expected fees come from the **Deposit** and **Transfer** sections of Assignment 1.
The tests use the balances before the transaction and cent-precision inputs.
Expected fees use the specified percentages without rounding to cents. JUnit uses a tolerance of `1e-9`.

### Deposit

Weak robust equivalence class analysis selects a representative from each of the eight fee rules.
Additional cases probe threshold equality and the next cent, and zero is included because the fee specification gives no positive minimum.
For the robustness cases, one monetary input is negative while the other remains nominal.
The specification gives no expected fee or exception for those four inputs. The calculator runs for each and Surefire records the observation as skipped.

| Class | Student | Amount ($) | Balance ($) | Rate | Representative |
|---|---|---|---|---|---|
| DS1 | true | >50 | >500 | 0.5% | D-S01 |
| DS2 | true | >50 | ≤500 | 0.25% | D-S02 |
| DS3 | true | ≤50 | >5000 | 0.5% | D-S03 |
| DS4 | true | ≤50 | ≤5000 | 0% | D-S04 |
| DN1 | false | >250 | >2500 | 0.8% | D-N01 |
| DN2 | false | >250 | ≤2500 | 0.4% | D-N02 |
| DN3 | false | ≤250 | >10000 | 0% | D-N03 |
| DN4 | false | ≤250 | ≤10000 | 0.1% | D-N04 |

### Transfer

Decision table analysis crosses the amount range, source balance and destination balance for each customer type.
The 16 rules each have an interior and a boundary representative. Nine additional cases probe thresholds and overlapping ranges.
Fee tests treat the three monetary inputs independently. The threshold comparisons place equality in the upper range.

| Rule | Student | Amount ($) | From ($) | To ($) | Rate |
|---|---|---|---|---|---|
| R01 | true | <200 | <2000 | <1000 | 0.1% |
| R02 | true | <200 | <2000 | ≥1000 | 0.05% |
| R03 | true | <200 | ≥2000 | <1000 | 0.05% |
| R04 | true | <200 | ≥2000 | ≥1000 | 0.025% |
| R05 | true | ≥200 | <2000 | <1000 | 0.05% |
| R06 | true | ≥200 | <2000 | ≥1000 | 0.025% |
| R07 | true | ≥200 | ≥2000 | <1000 | 0.25% |
| R08 | true | ≥200 | ≥2000 | ≥1000 | 0.125% |
| R09 | false | <100 | <4000 | <2000 | 0.2% |
| R10 | false | <100 | <4000 | ≥2000 | 0.1% |
| R11 | false | <100 | ≥4000 | <2000 | 1% |
| R12 | false | <100 | ≥4000 | ≥2000 | 0.5% |
| R13 | false | ≥100 | <2000 | <1000 | 0.2% |
| R14 | false | ≥100 | <2000 | ≥1000 | 0.1% |
| R15 | false | ≥100 | ≥2000 | <1000 | 0.5% |
| R16 | false | ≥100 | ≥2000 | ≥1000 | 0.25% |

## Cases and results

The clean Java 11 run produced 4 passes, 16 assertion failures and 4 observations for deposit.
All 41 transfer cases failed assertions. Both suites reported zero errors.
Amounts and fees are in dollars.

### Deposit results

| ID | Class | Amount | Balance | Student | Expected fee | Observed fee | Result |
|---|---|---|---|---|---|---|---|
| D-S01 | DS1 | 100 | 1000 | true | 0.500 | 0.2 | Fail (D-F01) |
| D-S02 | DS2 | 100 | 400 | true | 0.2500 | 0.2 | Fail (D-F01) |
| D-S03 | DS3 | 25 | 6000 | true | 0.125 | Matches expected | Pass |
| D-S04 | DS4 | 25 | 400 | true | 0 | 0.05 | Fail (D-F02) |
| D-N01 | DN1 | 400 | 3000 | false | 3.200 | 0 | Fail (D-F03) |
| D-N02 | DN2 | 400 | 2000 | false | 1.600 | 0 | Fail (D-F03) |
| D-N03 | DN3 | 100 | 11000 | false | 0 | 0.5 | Fail (D-F04) |
| D-N04 | DN4 | 100 | 9000 | false | 0.100 | 0 | Fail (D-F05) |
| D-S05 | DS4 | 50 | 5000 | true | 0 | 0.1 | Fail (D-F02) |
| D-S06 | DS3 | 50 | 5000.01 | true | 0.250 | Matches expected | Pass |
| D-S07 | DS2 | 50.01 | 500 | true | 0.125025 | 0.10002 | Fail (D-F01) |
| D-S08 | DS1 | 50.01 | 500.01 | true | 0.25005 | 0.10002 | Fail (D-F01) |
| D-N05 | DN4 | 250 | 10000 | false | 0.250 | 0 | Fail (D-F05) |
| D-N06 | DN3 | 250 | 10000.01 | false | 0 | 1.25 | Fail (D-F04) |
| D-N07 | DN2 | 250.01 | 2500 | false | 1.00004 | 0 | Fail (D-F03) |
| D-N08 | DN1 | 250.01 | 2500.01 | false | 2.00008 | 0 | Fail (D-F03) |
| D-S09 | DS4 | 0 | 0 | true | 0 | Matches expected | Pass |
| D-N09 | DN4 | 0 | 0 | false | 0.000 | Matches expected | Pass |
| D-S10 | DS1 | 100 | 3000 | true | 0.500 | 0.2 | Fail (D-F01) |
| D-N10 | DN4 | 100 | 400 | false | 0.100 | 0 | Fail (D-F05) |
| D-X01 | Negative amount | -1 | 3000 | true | Unspecified | -0.002 | Observation |
| D-X02 | Negative balance | 100 | -1 | true | Unspecified | 0.2 | Observation |
| D-X03 | Negative amount | -1 | 3000 | false | Unspecified | -0 | Observation |
| D-X04 | Negative balance | 100 | -1 | false | Unspecified | 0 | Observation |

### Transfer results

| ID | Rule | Amount | From | To | Student | Expected fee | Observed fee | Result |
|---|---|---|---|---|---|---|---|---|
| T-R01I | R01 | 50.0 | 1000.0 | 500.0 | true | 0.0500 | 2.5 | Fail (T-F01) |
| T-R01B | R01 | 199.99 | 1999.99 | 999.99 | true | 0.19999 | 4.999750000000001 | Fail (T-F01) |
| T-R02I | R02 | 50.0 | 1000.0 | 2000 | true | 0.02500 | 1.25 | Fail (T-F01) |
| T-R02B | R02 | 199.99 | 1999.99 | 1000 | true | 0.099995 | 2.4998750000000003 | Fail (T-F01) |
| T-R03I | R03 | 50.0 | 3000 | 500.0 | true | 0.02500 | 2.5 | Fail (T-F01) |
| T-R03B | R03 | 199.99 | 2000 | 999.99 | true | 0.099995 | 4.999750000000001 | Fail (T-F01) |
| T-R04I | R04 | 50.0 | 3000 | 2000 | true | 0.012500 | 1.25 | Fail (T-F01) |
| T-R04B | R04 | 199.99 | 2000 | 1000 | true | 0.0499975 | 2.4998750000000003 | Fail (T-F01) |
| T-R05I | R05 | 300 | 1000.0 | 500.0 | true | 0.1500 | 7.5 | Fail (T-F02) |
| T-R05B | R05 | 200 | 1999.99 | 999.99 | true | 0.1000 | 5 | Fail (T-F02) |
| T-R06I | R06 | 300 | 1000.0 | 2000 | true | 0.07500 | 3.75 | Fail (T-F02) |
| T-R06B | R06 | 200 | 1999.99 | 1000 | true | 0.05000 | 2.5 | Fail (T-F02) |
| T-R07I | R07 | 300 | 3000 | 500.0 | true | 0.7500 | 7.5 | Fail (T-F02) |
| T-R07B | R07 | 200 | 2000 | 999.99 | true | 0.5000 | 5 | Fail (T-F02) |
| T-R08I | R08 | 300 | 3000 | 2000 | true | 0.37500 | 3.75 | Fail (T-F02) |
| T-R08B | R08 | 200 | 2000 | 1000 | true | 0.25000 | 2.5 | Fail (T-F02) |
| T-R09I | R09 | 25.0 | 2000.0 | 1000.0 | false | 0.0500 | 1.25 | Fail (T-F03) |
| T-R09B | R09 | 99.99 | 3999.99 | 1999.99 | false | 0.19998 | 4.9995 | Fail (T-F03) |
| T-R10I | R10 | 25.0 | 2000.0 | 3000 | false | 0.0250 | 1.25 | Fail (T-F03) |
| T-R10B | R10 | 99.99 | 3999.99 | 2000 | false | 0.09999 | 4.9995 | Fail (T-F03) |
| T-R11I | R11 | 25.0 | 5000 | 1000.0 | false | 0.250 | 1.25 | Fail (T-F03) |
| T-R11B | R11 | 99.99 | 4000 | 1999.99 | false | 0.9999 | 4.9995 | Fail (T-F03) |
| T-R12I | R12 | 25.0 | 5000 | 3000 | false | 0.1250 | 1.25 | Fail (T-F03) |
| T-R12B | R12 | 99.99 | 4000 | 2000 | false | 0.49995 | 4.9995 | Fail (T-F03) |
| T-R13I | R13 | 200 | 1000.0 | 500.0 | false | 0.400 | 10 | Fail (T-F04) |
| T-R13B | R13 | 100 | 1999.99 | 999.99 | false | 0.200 | 5 | Fail (T-F04) |
| T-R14I | R14 | 200 | 1000.0 | 2000 | false | 0.200 | 11 | Fail (T-F04) |
| T-R14B | R14 | 100 | 1999.99 | 1000 | false | 0.100 | 5.5 | Fail (T-F04) |
| T-R15I | R15 | 200 | 3000 | 500.0 | false | 1.000 | 10 | Fail (T-F04) |
| T-R15B | R15 | 100 | 2000 | 999.99 | false | 0.500 | 5 | Fail (T-F04) |
| T-R16I | R16 | 200 | 3000 | 2000 | false | 0.5000 | 11 | Fail (T-F04) |
| T-R16B | R16 | 100 | 2000 | 1000 | false | 0.2500 | 5.5 | Fail (T-F04) |
| T-X01 | R01 | 150 | 1500 | 500 | true | 0.150 | 3.75 | Fail (T-F01) |
| T-X02 | R03 | 150 | 2500 | 500 | true | 0.0750 | 3.75 | Fail (T-F01) |
| T-X03 | R05 | 200 | 1500 | 500 | true | 0.1000 | 5 | Fail (T-F02) |
| T-X04 | R08 | 200 | 2000 | 1000 | true | 0.25000 | 2.5 | Fail (T-F02) |
| T-X05 | R09 | 50 | 3000 | 1500 | false | 0.100 | 2.5 | Fail (T-F03) |
| T-X06 | R11 | 50 | 4000 | 1500 | false | 0.50 | 2.5 | Fail (T-F03) |
| T-X07 | R13 | 100 | 1500 | 500 | false | 0.200 | 5 | Fail (T-F04) |
| T-X08 | R15 | 100 | 2000 | 500 | false | 0.500 | 5 | Fail (T-F04) |
| T-X09 | R16 | 100 | 2000 | 1000 | false | 0.2500 | 5.5 | Fail (T-F04) |

## Fault localization

| Failure ID | Failing cases | Suspected location | Justification |
|---|---|---|---|
| D-F01 | D-S01, D-S02, D-S07, D-S08, D-S10 | [`calculateDepositInterest`](../src/main/java/bank/utils/FeesCalculator.java#L39) | The student amount condition uses > 100. These deposits exceed 50 but enter the low-deposit branch. |
| D-F02 | D-S04, D-S05 | [`calculateDepositInterest`](../src/main/java/bank/utils/FeesCalculator.java#L50) | The low student amount and balance branch charges 0.2% where the fee should be zero. |
| D-F03 | D-N01, D-N02, D-N07, D-N08 | [`calculateDepositInterest`](../src/main/java/bank/utils/FeesCalculator.java#L54) | The nonstudent amount condition uses > 500. These deposits exceed 250 but enter the low-deposit branch. |
| D-F04 | D-N03, D-N06 | [`calculateDepositInterest`](../src/main/java/bank/utils/FeesCalculator.java#L62) | The branch above a nonstudent balance of 10000 charges 0.5% on small deposits where the fee should be zero. |
| D-F05 | D-N04, D-N05, D-N10 | [`calculateDepositInterest`](../src/main/java/bank/utils/FeesCalculator.java#L64) | The branch at or below a nonstudent balance of 10000 charges zero on small deposits where the fee should be 0.1%. |
| T-F01 | T-R01I, T-R01B, T-R02I, T-R02B, T-R03I, T-R03B, T-R04I, T-R04B, T-X01, T-X02 | [`calculateTransferFee`](../src/main/java/bank/utils/FeesCalculator.java#L80) | Student transfers below 200 encounter a cutoff of 100 and a source threshold of 1000. The rates in the small-transfer branch also exceed the specified rates. |
| T-F02 | T-R05I, T-R05B, T-R06I, T-R06B, T-R07I, T-R07B, T-R08I, T-R08B, T-X03, T-X04 | [`calculateTransferFee`](../src/main/java/bank/utils/FeesCalculator.java#L95) | For student transfers at or above 200, the source threshold is 1000 instead of 2000 and the four rates are ten times the specified rates. |
| T-F03 | T-R09I, T-R09B, T-R10I, T-R10B, T-R11I, T-R11B, T-R12I, T-R12B, T-X05, T-X06 | [`calculateTransferFee`](../src/main/java/bank/utils/FeesCalculator.java#L111) | For nonstudent transfers below 100, the source and destination cutoffs are both 1000 instead of 4000 and 2000, and the four rates are ten times the specified rates. |
| T-F04 | T-R13I, T-R13B, T-R14I, T-R14B, T-R15I, T-R15B, T-R16I, T-R16B, T-X07, T-X08, T-X09 | [`calculateTransferFee`](../src/main/java/bank/utils/FeesCalculator.java#L125) | For nonstudent transfers at or above 100, the source cutoff is 1000 instead of 2000 and the rates differ, including 5.5% instead of 0.25% in the last branch. |

The [deposit and transfer failure log](deposit-transfer-failure-log.csv) retains the failure IDs and timing evidence.

Each log row repeats the same batch timings from the integrated review. The automated run time includes the entire Maven run.
