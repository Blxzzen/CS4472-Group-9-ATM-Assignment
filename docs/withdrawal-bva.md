# Assignment 1 - Withdrawal fee boundary analysis

## Process and assumptions

The target is `bank.utils.FeesCalculator.calculateWithdrawalFee`. Expected
fees come from Assignment 1, **Withdrawal**. The required technique is robust
worst-case boundary value analysis.

- Students pay no fee on weekdays and 0.1% of the amount on weekends.
- Nonstudents pay 0.3% when the balance is below $1000, 0.1% from $1000
  inclusive to $5000 exclusive, and no fee at $5000 or above.
- Balances are the balances before withdrawal. The amount and balance are
  independent inputs to the fee calculation. Cash denominations, sufficient
  funds and the Session daily limit are separate from this target.
- Days follow `java.util.Calendar`, as supplied by `BankWithdrawal`:
  Sunday = 1, Monday = 2, through Saturday = 7. Both weekend days and all
  weekdays are included. Both student values are included.
- Monetary inputs use cent precision. Expected fees use the specified
  percentages without rounding to cents; JUnit comparisons use an
  absolute tolerance of `1e-9`.

The specification gives no finite upper bound for the amount or balance.
For a finite grid, the sampling ranges are $0–$1000 for amount and
$0–$10000 for balance, with nominal values of $500 and $1000 respectively.
These upper endpoints are test-design choices, not account limits.
$1000.01 amounts and $10000.01 balances therefore remain valid fee inputs.
Zero is included as a fee-calculation input because no positive-minimum
rule is given for this method.

## Boundary values and combinations

The grid includes one-cent neighbors of the monetary boundaries, the two
balance tier boundaries, and every combination of the selected inputs.
The single-fault restriction in the assignment applies to Session.

| Input | Selected values | Purpose |
|---|---|---|
| Amount | -0.01, 0, 0.01, 500, 999.99, 1000, 1000.01 | Sampling endpoints and neighbors, plus nominal amount |
| Balance | -0.01, 0, 0.01, 999.99, 1000, 1000.01, 4999.99, 5000, 5000.01, 9999.99, 10000, 10000.01 | Sampling endpoints and neighbors, including both fee tier boundaries |
| Day | 0, 1, 2, 3, 4, 5, 6, 7, 8 | Every valid day and the two adjacent invalid values |
| Student | true, false | Both customer categories |

This gives **7 × 12 × 9 × 2 = 1512 combinations**. Of these,
**6 × 11 × 7 × 2 = 924** have nonnegative monetary inputs and a valid day,
so their expected fee follows the written rules.

The remaining **588 combinations** contain a negative monetary input or an
out-of-range day. The specification gives no expected behavior or exception
for these inputs, so they have no pass/fail verdict.

The complete input grid and expected fees are in
[`withdrawal.csv`](../src/test/resources/assignment1/withdrawal.csv).
Case IDs follow CSV row order, excluding the header. Individual execution
results and robustness observations are recorded in Surefire XML.

## Reference cases

These six reference cases cover the student rules and nonstudent tier
boundaries.

| Case ID | Amount ($) | Balance ($) | Student | Day | Expected fee ($) | Observed fee ($) | Result |
|---|---:|---:|---|---|---:|---:|---|
| W-1110 | 500 | 999.99 | false | Monday (2) | 1.50 | 1.00 | Fail (W-F02) |
| W-1119 | 500 | 1000 | false | Monday (2) | 0.50 | 0.50 | Pass |
| W-1137 | 500 | 4999.99 | false | Monday (2) | 0.50 | 0.50 | Pass |
| W-1146 | 500 | 5000 | false | Monday (2) | 0.00 | 0.50 | Fail (W-F03) |
| W-0363 | 500 | 1000 | true | Monday (2) | 0.00 | 0.50 | Fail (W-F01) |
| W-0368 | 500 | 1000 | true | Saturday (7) | 0.50 | 0.00 | Fail (W-F01) |

## Execution results

Java 11.0.32.1 and Maven 3.9.9. Checkstyle reported zero violations.

```bash
mvn -B checkstyle:check
mvn -B -Dtest=WithdrawalFeeTest test
mvn -B clean -Dmaven.test.failure.ignore=true test
```

The targeted withdrawal run and clean suite agreed on all 1512 invocations:
**329 passed, 595 failed assertions, 588 skipped, zero errors**. All 924 cases
with specified fees were asserted. Each of the 588 robustness cases was
called and its returned fee recorded before JUnit marked it skipped because
there is no specified expected outcome.

The strict targeted command exited with test failures, as expected. The clean
suite used the assignment's informational mode and generated JaCoCo coverage.
The [integrated run](assignment1.md#cases-and-results) reproduced the withdrawal results alongside the other Assignment 1 suites.

The following groups cover the entire grid. All values are drawn from the
selected sets above; the first seven rows use nonnegative monetary inputs
and valid days. `a` is the withdrawal amount.

| Case group | Cases | Expected fee | Observed fee | Result |
|---|---:|---|---|---|
| Zero amount, either customer category | 154 | 0 | 0 | Pass |
| Student, positive amount, weekday | 275 | 0 | a × 0.001 | Fail (W-F01) |
| Student, positive amount, weekend | 110 | a × 0.001 | 0 | Fail (W-F01) |
| Nonstudent, positive amount, balance <1000 | 105 | a × 0.003 | a × 0.002 | Fail (W-F02) |
| Nonstudent, positive amount, 1000 ≤ balance <5000 | 105 | a × 0.001 | a × 0.001 | Pass |
| Nonstudent, positive amount, 5000 ≤ balance <10000 | 105 | 0 | a × 0.001 | Fail (W-F03) |
| Nonstudent, positive amount, balance ≥10000 | 70 | 0 | 0 | Pass |
| Negative monetary input or day outside 1–7 | 588 | Unspecified | Returned fees, recorded in Surefire XML | Observation (skipped) |

## Fault localization

All locations below are in `FeesCalculator.calculateWithdrawalFee`.
The groups above identify which cases expose each fault.

| Failure ID | Cases | Suspected location | Justification |
|---|---:|---|---|
| W-F01 | 385 | [Student rates, lines 12–15](../src/main/java/bank/utils/FeesCalculator.java#L12) | Weekend and weekday rates are reversed. W-0363 returns 0.50 instead of 0 on Monday; W-0368 returns 0 instead of 0.50 on Saturday. |
| W-F02 | 105 | [Low-balance rate, line 19](../src/main/java/bank/utils/FeesCalculator.java#L19) | The nonstudent rate below $1000 is 0.002 instead of 0.003. W-1110 returns 1.00 instead of 1.50. |
| W-F03 | 105 | [Middle-tier condition, line 20](../src/main/java/bank/utils/FeesCalculator.java#L20) | The condition extends the 0.1% tier to $10000 instead of $5000. W-1146 returns 0.50 instead of 0 at balance $5000. |

Zero-amount cases cannot expose rate differences because their fee is zero.

The [failure log](withdrawal-failure-log.csv) lists representative cases and
the measured automated run and batch-localization times. Each row refers to
the same batch, not a separate timing measurement.

## Complete case results

Amounts and fees are in dollars. Observations have no specified pass/fail outcome.

| ID | Amount | Balance | Student | Day | Expected fee | Observed fee | Result |
|---|---|---|---|---|---|---|---|
| W-0001 | -0.01 | -0.01 | true | 0 | Unspecified | -0.00001 | Observation |
| W-0002 | -0.01 | -0.01 | true | 1 | Unspecified | -0 | Observation |
| W-0003 | -0.01 | -0.01 | true | 2 | Unspecified | -0.00001 | Observation |
| W-0004 | -0.01 | -0.01 | true | 3 | Unspecified | -0.00001 | Observation |
| W-0005 | -0.01 | -0.01 | true | 4 | Unspecified | -0.00001 | Observation |
| W-0006 | -0.01 | -0.01 | true | 5 | Unspecified | -0.00001 | Observation |
| W-0007 | -0.01 | -0.01 | true | 6 | Unspecified | -0.00001 | Observation |
| W-0008 | -0.01 | -0.01 | true | 7 | Unspecified | -0 | Observation |
| W-0009 | -0.01 | -0.01 | true | 8 | Unspecified | -0.00001 | Observation |
| W-0010 | -0.01 | 0 | true | 0 | Unspecified | -0.00001 | Observation |
| W-0011 | -0.01 | 0 | true | 1 | Unspecified | -0 | Observation |
| W-0012 | -0.01 | 0 | true | 2 | Unspecified | -0.00001 | Observation |
| W-0013 | -0.01 | 0 | true | 3 | Unspecified | -0.00001 | Observation |
| W-0014 | -0.01 | 0 | true | 4 | Unspecified | -0.00001 | Observation |
| W-0015 | -0.01 | 0 | true | 5 | Unspecified | -0.00001 | Observation |
| W-0016 | -0.01 | 0 | true | 6 | Unspecified | -0.00001 | Observation |
| W-0017 | -0.01 | 0 | true | 7 | Unspecified | -0 | Observation |
| W-0018 | -0.01 | 0 | true | 8 | Unspecified | -0.00001 | Observation |
| W-0019 | -0.01 | 0.01 | true | 0 | Unspecified | -0.00001 | Observation |
| W-0020 | -0.01 | 0.01 | true | 1 | Unspecified | -0 | Observation |
| W-0021 | -0.01 | 0.01 | true | 2 | Unspecified | -0.00001 | Observation |
| W-0022 | -0.01 | 0.01 | true | 3 | Unspecified | -0.00001 | Observation |
| W-0023 | -0.01 | 0.01 | true | 4 | Unspecified | -0.00001 | Observation |
| W-0024 | -0.01 | 0.01 | true | 5 | Unspecified | -0.00001 | Observation |
| W-0025 | -0.01 | 0.01 | true | 6 | Unspecified | -0.00001 | Observation |
| W-0026 | -0.01 | 0.01 | true | 7 | Unspecified | -0 | Observation |
| W-0027 | -0.01 | 0.01 | true | 8 | Unspecified | -0.00001 | Observation |
| W-0028 | -0.01 | 999.99 | true | 0 | Unspecified | -0.00001 | Observation |
| W-0029 | -0.01 | 999.99 | true | 1 | Unspecified | -0 | Observation |
| W-0030 | -0.01 | 999.99 | true | 2 | Unspecified | -0.00001 | Observation |
| W-0031 | -0.01 | 999.99 | true | 3 | Unspecified | -0.00001 | Observation |
| W-0032 | -0.01 | 999.99 | true | 4 | Unspecified | -0.00001 | Observation |
| W-0033 | -0.01 | 999.99 | true | 5 | Unspecified | -0.00001 | Observation |
| W-0034 | -0.01 | 999.99 | true | 6 | Unspecified | -0.00001 | Observation |
| W-0035 | -0.01 | 999.99 | true | 7 | Unspecified | -0 | Observation |
| W-0036 | -0.01 | 999.99 | true | 8 | Unspecified | -0.00001 | Observation |
| W-0037 | -0.01 | 1000 | true | 0 | Unspecified | -0.00001 | Observation |
| W-0038 | -0.01 | 1000 | true | 1 | Unspecified | -0 | Observation |
| W-0039 | -0.01 | 1000 | true | 2 | Unspecified | -0.00001 | Observation |
| W-0040 | -0.01 | 1000 | true | 3 | Unspecified | -0.00001 | Observation |
| W-0041 | -0.01 | 1000 | true | 4 | Unspecified | -0.00001 | Observation |
| W-0042 | -0.01 | 1000 | true | 5 | Unspecified | -0.00001 | Observation |
| W-0043 | -0.01 | 1000 | true | 6 | Unspecified | -0.00001 | Observation |
| W-0044 | -0.01 | 1000 | true | 7 | Unspecified | -0 | Observation |
| W-0045 | -0.01 | 1000 | true | 8 | Unspecified | -0.00001 | Observation |
| W-0046 | -0.01 | 1000.01 | true | 0 | Unspecified | -0.00001 | Observation |
| W-0047 | -0.01 | 1000.01 | true | 1 | Unspecified | -0 | Observation |
| W-0048 | -0.01 | 1000.01 | true | 2 | Unspecified | -0.00001 | Observation |
| W-0049 | -0.01 | 1000.01 | true | 3 | Unspecified | -0.00001 | Observation |
| W-0050 | -0.01 | 1000.01 | true | 4 | Unspecified | -0.00001 | Observation |
| W-0051 | -0.01 | 1000.01 | true | 5 | Unspecified | -0.00001 | Observation |
| W-0052 | -0.01 | 1000.01 | true | 6 | Unspecified | -0.00001 | Observation |
| W-0053 | -0.01 | 1000.01 | true | 7 | Unspecified | -0 | Observation |
| W-0054 | -0.01 | 1000.01 | true | 8 | Unspecified | -0.00001 | Observation |
| W-0055 | -0.01 | 4999.99 | true | 0 | Unspecified | -0.00001 | Observation |
| W-0056 | -0.01 | 4999.99 | true | 1 | Unspecified | -0 | Observation |
| W-0057 | -0.01 | 4999.99 | true | 2 | Unspecified | -0.00001 | Observation |
| W-0058 | -0.01 | 4999.99 | true | 3 | Unspecified | -0.00001 | Observation |
| W-0059 | -0.01 | 4999.99 | true | 4 | Unspecified | -0.00001 | Observation |
| W-0060 | -0.01 | 4999.99 | true | 5 | Unspecified | -0.00001 | Observation |
| W-0061 | -0.01 | 4999.99 | true | 6 | Unspecified | -0.00001 | Observation |
| W-0062 | -0.01 | 4999.99 | true | 7 | Unspecified | -0 | Observation |
| W-0063 | -0.01 | 4999.99 | true | 8 | Unspecified | -0.00001 | Observation |
| W-0064 | -0.01 | 5000 | true | 0 | Unspecified | -0.00001 | Observation |
| W-0065 | -0.01 | 5000 | true | 1 | Unspecified | -0 | Observation |
| W-0066 | -0.01 | 5000 | true | 2 | Unspecified | -0.00001 | Observation |
| W-0067 | -0.01 | 5000 | true | 3 | Unspecified | -0.00001 | Observation |
| W-0068 | -0.01 | 5000 | true | 4 | Unspecified | -0.00001 | Observation |
| W-0069 | -0.01 | 5000 | true | 5 | Unspecified | -0.00001 | Observation |
| W-0070 | -0.01 | 5000 | true | 6 | Unspecified | -0.00001 | Observation |
| W-0071 | -0.01 | 5000 | true | 7 | Unspecified | -0 | Observation |
| W-0072 | -0.01 | 5000 | true | 8 | Unspecified | -0.00001 | Observation |
| W-0073 | -0.01 | 5000.01 | true | 0 | Unspecified | -0.00001 | Observation |
| W-0074 | -0.01 | 5000.01 | true | 1 | Unspecified | -0 | Observation |
| W-0075 | -0.01 | 5000.01 | true | 2 | Unspecified | -0.00001 | Observation |
| W-0076 | -0.01 | 5000.01 | true | 3 | Unspecified | -0.00001 | Observation |
| W-0077 | -0.01 | 5000.01 | true | 4 | Unspecified | -0.00001 | Observation |
| W-0078 | -0.01 | 5000.01 | true | 5 | Unspecified | -0.00001 | Observation |
| W-0079 | -0.01 | 5000.01 | true | 6 | Unspecified | -0.00001 | Observation |
| W-0080 | -0.01 | 5000.01 | true | 7 | Unspecified | -0 | Observation |
| W-0081 | -0.01 | 5000.01 | true | 8 | Unspecified | -0.00001 | Observation |
| W-0082 | -0.01 | 9999.99 | true | 0 | Unspecified | -0.00001 | Observation |
| W-0083 | -0.01 | 9999.99 | true | 1 | Unspecified | -0 | Observation |
| W-0084 | -0.01 | 9999.99 | true | 2 | Unspecified | -0.00001 | Observation |
| W-0085 | -0.01 | 9999.99 | true | 3 | Unspecified | -0.00001 | Observation |
| W-0086 | -0.01 | 9999.99 | true | 4 | Unspecified | -0.00001 | Observation |
| W-0087 | -0.01 | 9999.99 | true | 5 | Unspecified | -0.00001 | Observation |
| W-0088 | -0.01 | 9999.99 | true | 6 | Unspecified | -0.00001 | Observation |
| W-0089 | -0.01 | 9999.99 | true | 7 | Unspecified | -0 | Observation |
| W-0090 | -0.01 | 9999.99 | true | 8 | Unspecified | -0.00001 | Observation |
| W-0091 | -0.01 | 10000 | true | 0 | Unspecified | -0.00001 | Observation |
| W-0092 | -0.01 | 10000 | true | 1 | Unspecified | -0 | Observation |
| W-0093 | -0.01 | 10000 | true | 2 | Unspecified | -0.00001 | Observation |
| W-0094 | -0.01 | 10000 | true | 3 | Unspecified | -0.00001 | Observation |
| W-0095 | -0.01 | 10000 | true | 4 | Unspecified | -0.00001 | Observation |
| W-0096 | -0.01 | 10000 | true | 5 | Unspecified | -0.00001 | Observation |
| W-0097 | -0.01 | 10000 | true | 6 | Unspecified | -0.00001 | Observation |
| W-0098 | -0.01 | 10000 | true | 7 | Unspecified | -0 | Observation |
| W-0099 | -0.01 | 10000 | true | 8 | Unspecified | -0.00001 | Observation |
| W-0100 | -0.01 | 10000.01 | true | 0 | Unspecified | -0.00001 | Observation |
| W-0101 | -0.01 | 10000.01 | true | 1 | Unspecified | -0 | Observation |
| W-0102 | -0.01 | 10000.01 | true | 2 | Unspecified | -0.00001 | Observation |
| W-0103 | -0.01 | 10000.01 | true | 3 | Unspecified | -0.00001 | Observation |
| W-0104 | -0.01 | 10000.01 | true | 4 | Unspecified | -0.00001 | Observation |
| W-0105 | -0.01 | 10000.01 | true | 5 | Unspecified | -0.00001 | Observation |
| W-0106 | -0.01 | 10000.01 | true | 6 | Unspecified | -0.00001 | Observation |
| W-0107 | -0.01 | 10000.01 | true | 7 | Unspecified | -0 | Observation |
| W-0108 | -0.01 | 10000.01 | true | 8 | Unspecified | -0.00001 | Observation |
| W-0109 | 0 | -0.01 | true | 0 | Unspecified | 0 | Observation |
| W-0110 | 0 | -0.01 | true | 1 | Unspecified | 0 | Observation |
| W-0111 | 0 | -0.01 | true | 2 | Unspecified | 0 | Observation |
| W-0112 | 0 | -0.01 | true | 3 | Unspecified | 0 | Observation |
| W-0113 | 0 | -0.01 | true | 4 | Unspecified | 0 | Observation |
| W-0114 | 0 | -0.01 | true | 5 | Unspecified | 0 | Observation |
| W-0115 | 0 | -0.01 | true | 6 | Unspecified | 0 | Observation |
| W-0116 | 0 | -0.01 | true | 7 | Unspecified | 0 | Observation |
| W-0117 | 0 | -0.01 | true | 8 | Unspecified | 0 | Observation |
| W-0118 | 0 | 0 | true | 0 | Unspecified | 0 | Observation |
| W-0119 | 0 | 0 | true | 1 | 0 | Matches expected | Pass |
| W-0120 | 0 | 0 | true | 2 | 0 | Matches expected | Pass |
| W-0121 | 0 | 0 | true | 3 | 0 | Matches expected | Pass |
| W-0122 | 0 | 0 | true | 4 | 0 | Matches expected | Pass |
| W-0123 | 0 | 0 | true | 5 | 0 | Matches expected | Pass |
| W-0124 | 0 | 0 | true | 6 | 0 | Matches expected | Pass |
| W-0125 | 0 | 0 | true | 7 | 0 | Matches expected | Pass |
| W-0126 | 0 | 0 | true | 8 | Unspecified | 0 | Observation |
| W-0127 | 0 | 0.01 | true | 0 | Unspecified | 0 | Observation |
| W-0128 | 0 | 0.01 | true | 1 | 0 | Matches expected | Pass |
| W-0129 | 0 | 0.01 | true | 2 | 0 | Matches expected | Pass |
| W-0130 | 0 | 0.01 | true | 3 | 0 | Matches expected | Pass |
| W-0131 | 0 | 0.01 | true | 4 | 0 | Matches expected | Pass |
| W-0132 | 0 | 0.01 | true | 5 | 0 | Matches expected | Pass |
| W-0133 | 0 | 0.01 | true | 6 | 0 | Matches expected | Pass |
| W-0134 | 0 | 0.01 | true | 7 | 0 | Matches expected | Pass |
| W-0135 | 0 | 0.01 | true | 8 | Unspecified | 0 | Observation |
| W-0136 | 0 | 999.99 | true | 0 | Unspecified | 0 | Observation |
| W-0137 | 0 | 999.99 | true | 1 | 0 | Matches expected | Pass |
| W-0138 | 0 | 999.99 | true | 2 | 0 | Matches expected | Pass |
| W-0139 | 0 | 999.99 | true | 3 | 0 | Matches expected | Pass |
| W-0140 | 0 | 999.99 | true | 4 | 0 | Matches expected | Pass |
| W-0141 | 0 | 999.99 | true | 5 | 0 | Matches expected | Pass |
| W-0142 | 0 | 999.99 | true | 6 | 0 | Matches expected | Pass |
| W-0143 | 0 | 999.99 | true | 7 | 0 | Matches expected | Pass |
| W-0144 | 0 | 999.99 | true | 8 | Unspecified | 0 | Observation |
| W-0145 | 0 | 1000 | true | 0 | Unspecified | 0 | Observation |
| W-0146 | 0 | 1000 | true | 1 | 0 | Matches expected | Pass |
| W-0147 | 0 | 1000 | true | 2 | 0 | Matches expected | Pass |
| W-0148 | 0 | 1000 | true | 3 | 0 | Matches expected | Pass |
| W-0149 | 0 | 1000 | true | 4 | 0 | Matches expected | Pass |
| W-0150 | 0 | 1000 | true | 5 | 0 | Matches expected | Pass |
| W-0151 | 0 | 1000 | true | 6 | 0 | Matches expected | Pass |
| W-0152 | 0 | 1000 | true | 7 | 0 | Matches expected | Pass |
| W-0153 | 0 | 1000 | true | 8 | Unspecified | 0 | Observation |
| W-0154 | 0 | 1000.01 | true | 0 | Unspecified | 0 | Observation |
| W-0155 | 0 | 1000.01 | true | 1 | 0 | Matches expected | Pass |
| W-0156 | 0 | 1000.01 | true | 2 | 0 | Matches expected | Pass |
| W-0157 | 0 | 1000.01 | true | 3 | 0 | Matches expected | Pass |
| W-0158 | 0 | 1000.01 | true | 4 | 0 | Matches expected | Pass |
| W-0159 | 0 | 1000.01 | true | 5 | 0 | Matches expected | Pass |
| W-0160 | 0 | 1000.01 | true | 6 | 0 | Matches expected | Pass |
| W-0161 | 0 | 1000.01 | true | 7 | 0 | Matches expected | Pass |
| W-0162 | 0 | 1000.01 | true | 8 | Unspecified | 0 | Observation |
| W-0163 | 0 | 4999.99 | true | 0 | Unspecified | 0 | Observation |
| W-0164 | 0 | 4999.99 | true | 1 | 0 | Matches expected | Pass |
| W-0165 | 0 | 4999.99 | true | 2 | 0 | Matches expected | Pass |
| W-0166 | 0 | 4999.99 | true | 3 | 0 | Matches expected | Pass |
| W-0167 | 0 | 4999.99 | true | 4 | 0 | Matches expected | Pass |
| W-0168 | 0 | 4999.99 | true | 5 | 0 | Matches expected | Pass |
| W-0169 | 0 | 4999.99 | true | 6 | 0 | Matches expected | Pass |
| W-0170 | 0 | 4999.99 | true | 7 | 0 | Matches expected | Pass |
| W-0171 | 0 | 4999.99 | true | 8 | Unspecified | 0 | Observation |
| W-0172 | 0 | 5000 | true | 0 | Unspecified | 0 | Observation |
| W-0173 | 0 | 5000 | true | 1 | 0 | Matches expected | Pass |
| W-0174 | 0 | 5000 | true | 2 | 0 | Matches expected | Pass |
| W-0175 | 0 | 5000 | true | 3 | 0 | Matches expected | Pass |
| W-0176 | 0 | 5000 | true | 4 | 0 | Matches expected | Pass |
| W-0177 | 0 | 5000 | true | 5 | 0 | Matches expected | Pass |
| W-0178 | 0 | 5000 | true | 6 | 0 | Matches expected | Pass |
| W-0179 | 0 | 5000 | true | 7 | 0 | Matches expected | Pass |
| W-0180 | 0 | 5000 | true | 8 | Unspecified | 0 | Observation |
| W-0181 | 0 | 5000.01 | true | 0 | Unspecified | 0 | Observation |
| W-0182 | 0 | 5000.01 | true | 1 | 0 | Matches expected | Pass |
| W-0183 | 0 | 5000.01 | true | 2 | 0 | Matches expected | Pass |
| W-0184 | 0 | 5000.01 | true | 3 | 0 | Matches expected | Pass |
| W-0185 | 0 | 5000.01 | true | 4 | 0 | Matches expected | Pass |
| W-0186 | 0 | 5000.01 | true | 5 | 0 | Matches expected | Pass |
| W-0187 | 0 | 5000.01 | true | 6 | 0 | Matches expected | Pass |
| W-0188 | 0 | 5000.01 | true | 7 | 0 | Matches expected | Pass |
| W-0189 | 0 | 5000.01 | true | 8 | Unspecified | 0 | Observation |
| W-0190 | 0 | 9999.99 | true | 0 | Unspecified | 0 | Observation |
| W-0191 | 0 | 9999.99 | true | 1 | 0 | Matches expected | Pass |
| W-0192 | 0 | 9999.99 | true | 2 | 0 | Matches expected | Pass |
| W-0193 | 0 | 9999.99 | true | 3 | 0 | Matches expected | Pass |
| W-0194 | 0 | 9999.99 | true | 4 | 0 | Matches expected | Pass |
| W-0195 | 0 | 9999.99 | true | 5 | 0 | Matches expected | Pass |
| W-0196 | 0 | 9999.99 | true | 6 | 0 | Matches expected | Pass |
| W-0197 | 0 | 9999.99 | true | 7 | 0 | Matches expected | Pass |
| W-0198 | 0 | 9999.99 | true | 8 | Unspecified | 0 | Observation |
| W-0199 | 0 | 10000 | true | 0 | Unspecified | 0 | Observation |
| W-0200 | 0 | 10000 | true | 1 | 0 | Matches expected | Pass |
| W-0201 | 0 | 10000 | true | 2 | 0 | Matches expected | Pass |
| W-0202 | 0 | 10000 | true | 3 | 0 | Matches expected | Pass |
| W-0203 | 0 | 10000 | true | 4 | 0 | Matches expected | Pass |
| W-0204 | 0 | 10000 | true | 5 | 0 | Matches expected | Pass |
| W-0205 | 0 | 10000 | true | 6 | 0 | Matches expected | Pass |
| W-0206 | 0 | 10000 | true | 7 | 0 | Matches expected | Pass |
| W-0207 | 0 | 10000 | true | 8 | Unspecified | 0 | Observation |
| W-0208 | 0 | 10000.01 | true | 0 | Unspecified | 0 | Observation |
| W-0209 | 0 | 10000.01 | true | 1 | 0 | Matches expected | Pass |
| W-0210 | 0 | 10000.01 | true | 2 | 0 | Matches expected | Pass |
| W-0211 | 0 | 10000.01 | true | 3 | 0 | Matches expected | Pass |
| W-0212 | 0 | 10000.01 | true | 4 | 0 | Matches expected | Pass |
| W-0213 | 0 | 10000.01 | true | 5 | 0 | Matches expected | Pass |
| W-0214 | 0 | 10000.01 | true | 6 | 0 | Matches expected | Pass |
| W-0215 | 0 | 10000.01 | true | 7 | 0 | Matches expected | Pass |
| W-0216 | 0 | 10000.01 | true | 8 | Unspecified | 0 | Observation |
| W-0217 | 0.01 | -0.01 | true | 0 | Unspecified | 0.00001 | Observation |
| W-0218 | 0.01 | -0.01 | true | 1 | Unspecified | 0 | Observation |
| W-0219 | 0.01 | -0.01 | true | 2 | Unspecified | 0.00001 | Observation |
| W-0220 | 0.01 | -0.01 | true | 3 | Unspecified | 0.00001 | Observation |
| W-0221 | 0.01 | -0.01 | true | 4 | Unspecified | 0.00001 | Observation |
| W-0222 | 0.01 | -0.01 | true | 5 | Unspecified | 0.00001 | Observation |
| W-0223 | 0.01 | -0.01 | true | 6 | Unspecified | 0.00001 | Observation |
| W-0224 | 0.01 | -0.01 | true | 7 | Unspecified | 0 | Observation |
| W-0225 | 0.01 | -0.01 | true | 8 | Unspecified | 0.00001 | Observation |
| W-0226 | 0.01 | 0 | true | 0 | Unspecified | 0.00001 | Observation |
| W-0227 | 0.01 | 0 | true | 1 | 0.00001 | 0 | Fail (W-F01) |
| W-0228 | 0.01 | 0 | true | 2 | 0 | 0.00001 | Fail (W-F01) |
| W-0229 | 0.01 | 0 | true | 3 | 0 | 0.00001 | Fail (W-F01) |
| W-0230 | 0.01 | 0 | true | 4 | 0 | 0.00001 | Fail (W-F01) |
| W-0231 | 0.01 | 0 | true | 5 | 0 | 0.00001 | Fail (W-F01) |
| W-0232 | 0.01 | 0 | true | 6 | 0 | 0.00001 | Fail (W-F01) |
| W-0233 | 0.01 | 0 | true | 7 | 0.00001 | 0 | Fail (W-F01) |
| W-0234 | 0.01 | 0 | true | 8 | Unspecified | 0.00001 | Observation |
| W-0235 | 0.01 | 0.01 | true | 0 | Unspecified | 0.00001 | Observation |
| W-0236 | 0.01 | 0.01 | true | 1 | 0.00001 | 0 | Fail (W-F01) |
| W-0237 | 0.01 | 0.01 | true | 2 | 0 | 0.00001 | Fail (W-F01) |
| W-0238 | 0.01 | 0.01 | true | 3 | 0 | 0.00001 | Fail (W-F01) |
| W-0239 | 0.01 | 0.01 | true | 4 | 0 | 0.00001 | Fail (W-F01) |
| W-0240 | 0.01 | 0.01 | true | 5 | 0 | 0.00001 | Fail (W-F01) |
| W-0241 | 0.01 | 0.01 | true | 6 | 0 | 0.00001 | Fail (W-F01) |
| W-0242 | 0.01 | 0.01 | true | 7 | 0.00001 | 0 | Fail (W-F01) |
| W-0243 | 0.01 | 0.01 | true | 8 | Unspecified | 0.00001 | Observation |
| W-0244 | 0.01 | 999.99 | true | 0 | Unspecified | 0.00001 | Observation |
| W-0245 | 0.01 | 999.99 | true | 1 | 0.00001 | 0 | Fail (W-F01) |
| W-0246 | 0.01 | 999.99 | true | 2 | 0 | 0.00001 | Fail (W-F01) |
| W-0247 | 0.01 | 999.99 | true | 3 | 0 | 0.00001 | Fail (W-F01) |
| W-0248 | 0.01 | 999.99 | true | 4 | 0 | 0.00001 | Fail (W-F01) |
| W-0249 | 0.01 | 999.99 | true | 5 | 0 | 0.00001 | Fail (W-F01) |
| W-0250 | 0.01 | 999.99 | true | 6 | 0 | 0.00001 | Fail (W-F01) |
| W-0251 | 0.01 | 999.99 | true | 7 | 0.00001 | 0 | Fail (W-F01) |
| W-0252 | 0.01 | 999.99 | true | 8 | Unspecified | 0.00001 | Observation |
| W-0253 | 0.01 | 1000 | true | 0 | Unspecified | 0.00001 | Observation |
| W-0254 | 0.01 | 1000 | true | 1 | 0.00001 | 0 | Fail (W-F01) |
| W-0255 | 0.01 | 1000 | true | 2 | 0 | 0.00001 | Fail (W-F01) |
| W-0256 | 0.01 | 1000 | true | 3 | 0 | 0.00001 | Fail (W-F01) |
| W-0257 | 0.01 | 1000 | true | 4 | 0 | 0.00001 | Fail (W-F01) |
| W-0258 | 0.01 | 1000 | true | 5 | 0 | 0.00001 | Fail (W-F01) |
| W-0259 | 0.01 | 1000 | true | 6 | 0 | 0.00001 | Fail (W-F01) |
| W-0260 | 0.01 | 1000 | true | 7 | 0.00001 | 0 | Fail (W-F01) |
| W-0261 | 0.01 | 1000 | true | 8 | Unspecified | 0.00001 | Observation |
| W-0262 | 0.01 | 1000.01 | true | 0 | Unspecified | 0.00001 | Observation |
| W-0263 | 0.01 | 1000.01 | true | 1 | 0.00001 | 0 | Fail (W-F01) |
| W-0264 | 0.01 | 1000.01 | true | 2 | 0 | 0.00001 | Fail (W-F01) |
| W-0265 | 0.01 | 1000.01 | true | 3 | 0 | 0.00001 | Fail (W-F01) |
| W-0266 | 0.01 | 1000.01 | true | 4 | 0 | 0.00001 | Fail (W-F01) |
| W-0267 | 0.01 | 1000.01 | true | 5 | 0 | 0.00001 | Fail (W-F01) |
| W-0268 | 0.01 | 1000.01 | true | 6 | 0 | 0.00001 | Fail (W-F01) |
| W-0269 | 0.01 | 1000.01 | true | 7 | 0.00001 | 0 | Fail (W-F01) |
| W-0270 | 0.01 | 1000.01 | true | 8 | Unspecified | 0.00001 | Observation |
| W-0271 | 0.01 | 4999.99 | true | 0 | Unspecified | 0.00001 | Observation |
| W-0272 | 0.01 | 4999.99 | true | 1 | 0.00001 | 0 | Fail (W-F01) |
| W-0273 | 0.01 | 4999.99 | true | 2 | 0 | 0.00001 | Fail (W-F01) |
| W-0274 | 0.01 | 4999.99 | true | 3 | 0 | 0.00001 | Fail (W-F01) |
| W-0275 | 0.01 | 4999.99 | true | 4 | 0 | 0.00001 | Fail (W-F01) |
| W-0276 | 0.01 | 4999.99 | true | 5 | 0 | 0.00001 | Fail (W-F01) |
| W-0277 | 0.01 | 4999.99 | true | 6 | 0 | 0.00001 | Fail (W-F01) |
| W-0278 | 0.01 | 4999.99 | true | 7 | 0.00001 | 0 | Fail (W-F01) |
| W-0279 | 0.01 | 4999.99 | true | 8 | Unspecified | 0.00001 | Observation |
| W-0280 | 0.01 | 5000 | true | 0 | Unspecified | 0.00001 | Observation |
| W-0281 | 0.01 | 5000 | true | 1 | 0.00001 | 0 | Fail (W-F01) |
| W-0282 | 0.01 | 5000 | true | 2 | 0 | 0.00001 | Fail (W-F01) |
| W-0283 | 0.01 | 5000 | true | 3 | 0 | 0.00001 | Fail (W-F01) |
| W-0284 | 0.01 | 5000 | true | 4 | 0 | 0.00001 | Fail (W-F01) |
| W-0285 | 0.01 | 5000 | true | 5 | 0 | 0.00001 | Fail (W-F01) |
| W-0286 | 0.01 | 5000 | true | 6 | 0 | 0.00001 | Fail (W-F01) |
| W-0287 | 0.01 | 5000 | true | 7 | 0.00001 | 0 | Fail (W-F01) |
| W-0288 | 0.01 | 5000 | true | 8 | Unspecified | 0.00001 | Observation |
| W-0289 | 0.01 | 5000.01 | true | 0 | Unspecified | 0.00001 | Observation |
| W-0290 | 0.01 | 5000.01 | true | 1 | 0.00001 | 0 | Fail (W-F01) |
| W-0291 | 0.01 | 5000.01 | true | 2 | 0 | 0.00001 | Fail (W-F01) |
| W-0292 | 0.01 | 5000.01 | true | 3 | 0 | 0.00001 | Fail (W-F01) |
| W-0293 | 0.01 | 5000.01 | true | 4 | 0 | 0.00001 | Fail (W-F01) |
| W-0294 | 0.01 | 5000.01 | true | 5 | 0 | 0.00001 | Fail (W-F01) |
| W-0295 | 0.01 | 5000.01 | true | 6 | 0 | 0.00001 | Fail (W-F01) |
| W-0296 | 0.01 | 5000.01 | true | 7 | 0.00001 | 0 | Fail (W-F01) |
| W-0297 | 0.01 | 5000.01 | true | 8 | Unspecified | 0.00001 | Observation |
| W-0298 | 0.01 | 9999.99 | true | 0 | Unspecified | 0.00001 | Observation |
| W-0299 | 0.01 | 9999.99 | true | 1 | 0.00001 | 0 | Fail (W-F01) |
| W-0300 | 0.01 | 9999.99 | true | 2 | 0 | 0.00001 | Fail (W-F01) |
| W-0301 | 0.01 | 9999.99 | true | 3 | 0 | 0.00001 | Fail (W-F01) |
| W-0302 | 0.01 | 9999.99 | true | 4 | 0 | 0.00001 | Fail (W-F01) |
| W-0303 | 0.01 | 9999.99 | true | 5 | 0 | 0.00001 | Fail (W-F01) |
| W-0304 | 0.01 | 9999.99 | true | 6 | 0 | 0.00001 | Fail (W-F01) |
| W-0305 | 0.01 | 9999.99 | true | 7 | 0.00001 | 0 | Fail (W-F01) |
| W-0306 | 0.01 | 9999.99 | true | 8 | Unspecified | 0.00001 | Observation |
| W-0307 | 0.01 | 10000 | true | 0 | Unspecified | 0.00001 | Observation |
| W-0308 | 0.01 | 10000 | true | 1 | 0.00001 | 0 | Fail (W-F01) |
| W-0309 | 0.01 | 10000 | true | 2 | 0 | 0.00001 | Fail (W-F01) |
| W-0310 | 0.01 | 10000 | true | 3 | 0 | 0.00001 | Fail (W-F01) |
| W-0311 | 0.01 | 10000 | true | 4 | 0 | 0.00001 | Fail (W-F01) |
| W-0312 | 0.01 | 10000 | true | 5 | 0 | 0.00001 | Fail (W-F01) |
| W-0313 | 0.01 | 10000 | true | 6 | 0 | 0.00001 | Fail (W-F01) |
| W-0314 | 0.01 | 10000 | true | 7 | 0.00001 | 0 | Fail (W-F01) |
| W-0315 | 0.01 | 10000 | true | 8 | Unspecified | 0.00001 | Observation |
| W-0316 | 0.01 | 10000.01 | true | 0 | Unspecified | 0.00001 | Observation |
| W-0317 | 0.01 | 10000.01 | true | 1 | 0.00001 | 0 | Fail (W-F01) |
| W-0318 | 0.01 | 10000.01 | true | 2 | 0 | 0.00001 | Fail (W-F01) |
| W-0319 | 0.01 | 10000.01 | true | 3 | 0 | 0.00001 | Fail (W-F01) |
| W-0320 | 0.01 | 10000.01 | true | 4 | 0 | 0.00001 | Fail (W-F01) |
| W-0321 | 0.01 | 10000.01 | true | 5 | 0 | 0.00001 | Fail (W-F01) |
| W-0322 | 0.01 | 10000.01 | true | 6 | 0 | 0.00001 | Fail (W-F01) |
| W-0323 | 0.01 | 10000.01 | true | 7 | 0.00001 | 0 | Fail (W-F01) |
| W-0324 | 0.01 | 10000.01 | true | 8 | Unspecified | 0.00001 | Observation |
| W-0325 | 500 | -0.01 | true | 0 | Unspecified | 0.5 | Observation |
| W-0326 | 500 | -0.01 | true | 1 | Unspecified | 0 | Observation |
| W-0327 | 500 | -0.01 | true | 2 | Unspecified | 0.5 | Observation |
| W-0328 | 500 | -0.01 | true | 3 | Unspecified | 0.5 | Observation |
| W-0329 | 500 | -0.01 | true | 4 | Unspecified | 0.5 | Observation |
| W-0330 | 500 | -0.01 | true | 5 | Unspecified | 0.5 | Observation |
| W-0331 | 500 | -0.01 | true | 6 | Unspecified | 0.5 | Observation |
| W-0332 | 500 | -0.01 | true | 7 | Unspecified | 0 | Observation |
| W-0333 | 500 | -0.01 | true | 8 | Unspecified | 0.5 | Observation |
| W-0334 | 500 | 0 | true | 0 | Unspecified | 0.5 | Observation |
| W-0335 | 500 | 0 | true | 1 | 0.5 | 0 | Fail (W-F01) |
| W-0336 | 500 | 0 | true | 2 | 0 | 0.5 | Fail (W-F01) |
| W-0337 | 500 | 0 | true | 3 | 0 | 0.5 | Fail (W-F01) |
| W-0338 | 500 | 0 | true | 4 | 0 | 0.5 | Fail (W-F01) |
| W-0339 | 500 | 0 | true | 5 | 0 | 0.5 | Fail (W-F01) |
| W-0340 | 500 | 0 | true | 6 | 0 | 0.5 | Fail (W-F01) |
| W-0341 | 500 | 0 | true | 7 | 0.5 | 0 | Fail (W-F01) |
| W-0342 | 500 | 0 | true | 8 | Unspecified | 0.5 | Observation |
| W-0343 | 500 | 0.01 | true | 0 | Unspecified | 0.5 | Observation |
| W-0344 | 500 | 0.01 | true | 1 | 0.5 | 0 | Fail (W-F01) |
| W-0345 | 500 | 0.01 | true | 2 | 0 | 0.5 | Fail (W-F01) |
| W-0346 | 500 | 0.01 | true | 3 | 0 | 0.5 | Fail (W-F01) |
| W-0347 | 500 | 0.01 | true | 4 | 0 | 0.5 | Fail (W-F01) |
| W-0348 | 500 | 0.01 | true | 5 | 0 | 0.5 | Fail (W-F01) |
| W-0349 | 500 | 0.01 | true | 6 | 0 | 0.5 | Fail (W-F01) |
| W-0350 | 500 | 0.01 | true | 7 | 0.5 | 0 | Fail (W-F01) |
| W-0351 | 500 | 0.01 | true | 8 | Unspecified | 0.5 | Observation |
| W-0352 | 500 | 999.99 | true | 0 | Unspecified | 0.5 | Observation |
| W-0353 | 500 | 999.99 | true | 1 | 0.5 | 0 | Fail (W-F01) |
| W-0354 | 500 | 999.99 | true | 2 | 0 | 0.5 | Fail (W-F01) |
| W-0355 | 500 | 999.99 | true | 3 | 0 | 0.5 | Fail (W-F01) |
| W-0356 | 500 | 999.99 | true | 4 | 0 | 0.5 | Fail (W-F01) |
| W-0357 | 500 | 999.99 | true | 5 | 0 | 0.5 | Fail (W-F01) |
| W-0358 | 500 | 999.99 | true | 6 | 0 | 0.5 | Fail (W-F01) |
| W-0359 | 500 | 999.99 | true | 7 | 0.5 | 0 | Fail (W-F01) |
| W-0360 | 500 | 999.99 | true | 8 | Unspecified | 0.5 | Observation |
| W-0361 | 500 | 1000 | true | 0 | Unspecified | 0.5 | Observation |
| W-0362 | 500 | 1000 | true | 1 | 0.5 | 0 | Fail (W-F01) |
| W-0363 | 500 | 1000 | true | 2 | 0 | 0.5 | Fail (W-F01) |
| W-0364 | 500 | 1000 | true | 3 | 0 | 0.5 | Fail (W-F01) |
| W-0365 | 500 | 1000 | true | 4 | 0 | 0.5 | Fail (W-F01) |
| W-0366 | 500 | 1000 | true | 5 | 0 | 0.5 | Fail (W-F01) |
| W-0367 | 500 | 1000 | true | 6 | 0 | 0.5 | Fail (W-F01) |
| W-0368 | 500 | 1000 | true | 7 | 0.5 | 0 | Fail (W-F01) |
| W-0369 | 500 | 1000 | true | 8 | Unspecified | 0.5 | Observation |
| W-0370 | 500 | 1000.01 | true | 0 | Unspecified | 0.5 | Observation |
| W-0371 | 500 | 1000.01 | true | 1 | 0.5 | 0 | Fail (W-F01) |
| W-0372 | 500 | 1000.01 | true | 2 | 0 | 0.5 | Fail (W-F01) |
| W-0373 | 500 | 1000.01 | true | 3 | 0 | 0.5 | Fail (W-F01) |
| W-0374 | 500 | 1000.01 | true | 4 | 0 | 0.5 | Fail (W-F01) |
| W-0375 | 500 | 1000.01 | true | 5 | 0 | 0.5 | Fail (W-F01) |
| W-0376 | 500 | 1000.01 | true | 6 | 0 | 0.5 | Fail (W-F01) |
| W-0377 | 500 | 1000.01 | true | 7 | 0.5 | 0 | Fail (W-F01) |
| W-0378 | 500 | 1000.01 | true | 8 | Unspecified | 0.5 | Observation |
| W-0379 | 500 | 4999.99 | true | 0 | Unspecified | 0.5 | Observation |
| W-0380 | 500 | 4999.99 | true | 1 | 0.5 | 0 | Fail (W-F01) |
| W-0381 | 500 | 4999.99 | true | 2 | 0 | 0.5 | Fail (W-F01) |
| W-0382 | 500 | 4999.99 | true | 3 | 0 | 0.5 | Fail (W-F01) |
| W-0383 | 500 | 4999.99 | true | 4 | 0 | 0.5 | Fail (W-F01) |
| W-0384 | 500 | 4999.99 | true | 5 | 0 | 0.5 | Fail (W-F01) |
| W-0385 | 500 | 4999.99 | true | 6 | 0 | 0.5 | Fail (W-F01) |
| W-0386 | 500 | 4999.99 | true | 7 | 0.5 | 0 | Fail (W-F01) |
| W-0387 | 500 | 4999.99 | true | 8 | Unspecified | 0.5 | Observation |
| W-0388 | 500 | 5000 | true | 0 | Unspecified | 0.5 | Observation |
| W-0389 | 500 | 5000 | true | 1 | 0.5 | 0 | Fail (W-F01) |
| W-0390 | 500 | 5000 | true | 2 | 0 | 0.5 | Fail (W-F01) |
| W-0391 | 500 | 5000 | true | 3 | 0 | 0.5 | Fail (W-F01) |
| W-0392 | 500 | 5000 | true | 4 | 0 | 0.5 | Fail (W-F01) |
| W-0393 | 500 | 5000 | true | 5 | 0 | 0.5 | Fail (W-F01) |
| W-0394 | 500 | 5000 | true | 6 | 0 | 0.5 | Fail (W-F01) |
| W-0395 | 500 | 5000 | true | 7 | 0.5 | 0 | Fail (W-F01) |
| W-0396 | 500 | 5000 | true | 8 | Unspecified | 0.5 | Observation |
| W-0397 | 500 | 5000.01 | true | 0 | Unspecified | 0.5 | Observation |
| W-0398 | 500 | 5000.01 | true | 1 | 0.5 | 0 | Fail (W-F01) |
| W-0399 | 500 | 5000.01 | true | 2 | 0 | 0.5 | Fail (W-F01) |
| W-0400 | 500 | 5000.01 | true | 3 | 0 | 0.5 | Fail (W-F01) |
| W-0401 | 500 | 5000.01 | true | 4 | 0 | 0.5 | Fail (W-F01) |
| W-0402 | 500 | 5000.01 | true | 5 | 0 | 0.5 | Fail (W-F01) |
| W-0403 | 500 | 5000.01 | true | 6 | 0 | 0.5 | Fail (W-F01) |
| W-0404 | 500 | 5000.01 | true | 7 | 0.5 | 0 | Fail (W-F01) |
| W-0405 | 500 | 5000.01 | true | 8 | Unspecified | 0.5 | Observation |
| W-0406 | 500 | 9999.99 | true | 0 | Unspecified | 0.5 | Observation |
| W-0407 | 500 | 9999.99 | true | 1 | 0.5 | 0 | Fail (W-F01) |
| W-0408 | 500 | 9999.99 | true | 2 | 0 | 0.5 | Fail (W-F01) |
| W-0409 | 500 | 9999.99 | true | 3 | 0 | 0.5 | Fail (W-F01) |
| W-0410 | 500 | 9999.99 | true | 4 | 0 | 0.5 | Fail (W-F01) |
| W-0411 | 500 | 9999.99 | true | 5 | 0 | 0.5 | Fail (W-F01) |
| W-0412 | 500 | 9999.99 | true | 6 | 0 | 0.5 | Fail (W-F01) |
| W-0413 | 500 | 9999.99 | true | 7 | 0.5 | 0 | Fail (W-F01) |
| W-0414 | 500 | 9999.99 | true | 8 | Unspecified | 0.5 | Observation |
| W-0415 | 500 | 10000 | true | 0 | Unspecified | 0.5 | Observation |
| W-0416 | 500 | 10000 | true | 1 | 0.5 | 0 | Fail (W-F01) |
| W-0417 | 500 | 10000 | true | 2 | 0 | 0.5 | Fail (W-F01) |
| W-0418 | 500 | 10000 | true | 3 | 0 | 0.5 | Fail (W-F01) |
| W-0419 | 500 | 10000 | true | 4 | 0 | 0.5 | Fail (W-F01) |
| W-0420 | 500 | 10000 | true | 5 | 0 | 0.5 | Fail (W-F01) |
| W-0421 | 500 | 10000 | true | 6 | 0 | 0.5 | Fail (W-F01) |
| W-0422 | 500 | 10000 | true | 7 | 0.5 | 0 | Fail (W-F01) |
| W-0423 | 500 | 10000 | true | 8 | Unspecified | 0.5 | Observation |
| W-0424 | 500 | 10000.01 | true | 0 | Unspecified | 0.5 | Observation |
| W-0425 | 500 | 10000.01 | true | 1 | 0.5 | 0 | Fail (W-F01) |
| W-0426 | 500 | 10000.01 | true | 2 | 0 | 0.5 | Fail (W-F01) |
| W-0427 | 500 | 10000.01 | true | 3 | 0 | 0.5 | Fail (W-F01) |
| W-0428 | 500 | 10000.01 | true | 4 | 0 | 0.5 | Fail (W-F01) |
| W-0429 | 500 | 10000.01 | true | 5 | 0 | 0.5 | Fail (W-F01) |
| W-0430 | 500 | 10000.01 | true | 6 | 0 | 0.5 | Fail (W-F01) |
| W-0431 | 500 | 10000.01 | true | 7 | 0.5 | 0 | Fail (W-F01) |
| W-0432 | 500 | 10000.01 | true | 8 | Unspecified | 0.5 | Observation |
| W-0433 | 999.99 | -0.01 | true | 0 | Unspecified | 0.99999 | Observation |
| W-0434 | 999.99 | -0.01 | true | 1 | Unspecified | 0 | Observation |
| W-0435 | 999.99 | -0.01 | true | 2 | Unspecified | 0.99999 | Observation |
| W-0436 | 999.99 | -0.01 | true | 3 | Unspecified | 0.99999 | Observation |
| W-0437 | 999.99 | -0.01 | true | 4 | Unspecified | 0.99999 | Observation |
| W-0438 | 999.99 | -0.01 | true | 5 | Unspecified | 0.99999 | Observation |
| W-0439 | 999.99 | -0.01 | true | 6 | Unspecified | 0.99999 | Observation |
| W-0440 | 999.99 | -0.01 | true | 7 | Unspecified | 0 | Observation |
| W-0441 | 999.99 | -0.01 | true | 8 | Unspecified | 0.99999 | Observation |
| W-0442 | 999.99 | 0 | true | 0 | Unspecified | 0.99999 | Observation |
| W-0443 | 999.99 | 0 | true | 1 | 0.99999 | 0 | Fail (W-F01) |
| W-0444 | 999.99 | 0 | true | 2 | 0 | 0.99999 | Fail (W-F01) |
| W-0445 | 999.99 | 0 | true | 3 | 0 | 0.99999 | Fail (W-F01) |
| W-0446 | 999.99 | 0 | true | 4 | 0 | 0.99999 | Fail (W-F01) |
| W-0447 | 999.99 | 0 | true | 5 | 0 | 0.99999 | Fail (W-F01) |
| W-0448 | 999.99 | 0 | true | 6 | 0 | 0.99999 | Fail (W-F01) |
| W-0449 | 999.99 | 0 | true | 7 | 0.99999 | 0 | Fail (W-F01) |
| W-0450 | 999.99 | 0 | true | 8 | Unspecified | 0.99999 | Observation |
| W-0451 | 999.99 | 0.01 | true | 0 | Unspecified | 0.99999 | Observation |
| W-0452 | 999.99 | 0.01 | true | 1 | 0.99999 | 0 | Fail (W-F01) |
| W-0453 | 999.99 | 0.01 | true | 2 | 0 | 0.99999 | Fail (W-F01) |
| W-0454 | 999.99 | 0.01 | true | 3 | 0 | 0.99999 | Fail (W-F01) |
| W-0455 | 999.99 | 0.01 | true | 4 | 0 | 0.99999 | Fail (W-F01) |
| W-0456 | 999.99 | 0.01 | true | 5 | 0 | 0.99999 | Fail (W-F01) |
| W-0457 | 999.99 | 0.01 | true | 6 | 0 | 0.99999 | Fail (W-F01) |
| W-0458 | 999.99 | 0.01 | true | 7 | 0.99999 | 0 | Fail (W-F01) |
| W-0459 | 999.99 | 0.01 | true | 8 | Unspecified | 0.99999 | Observation |
| W-0460 | 999.99 | 999.99 | true | 0 | Unspecified | 0.99999 | Observation |
| W-0461 | 999.99 | 999.99 | true | 1 | 0.99999 | 0 | Fail (W-F01) |
| W-0462 | 999.99 | 999.99 | true | 2 | 0 | 0.99999 | Fail (W-F01) |
| W-0463 | 999.99 | 999.99 | true | 3 | 0 | 0.99999 | Fail (W-F01) |
| W-0464 | 999.99 | 999.99 | true | 4 | 0 | 0.99999 | Fail (W-F01) |
| W-0465 | 999.99 | 999.99 | true | 5 | 0 | 0.99999 | Fail (W-F01) |
| W-0466 | 999.99 | 999.99 | true | 6 | 0 | 0.99999 | Fail (W-F01) |
| W-0467 | 999.99 | 999.99 | true | 7 | 0.99999 | 0 | Fail (W-F01) |
| W-0468 | 999.99 | 999.99 | true | 8 | Unspecified | 0.99999 | Observation |
| W-0469 | 999.99 | 1000 | true | 0 | Unspecified | 0.99999 | Observation |
| W-0470 | 999.99 | 1000 | true | 1 | 0.99999 | 0 | Fail (W-F01) |
| W-0471 | 999.99 | 1000 | true | 2 | 0 | 0.99999 | Fail (W-F01) |
| W-0472 | 999.99 | 1000 | true | 3 | 0 | 0.99999 | Fail (W-F01) |
| W-0473 | 999.99 | 1000 | true | 4 | 0 | 0.99999 | Fail (W-F01) |
| W-0474 | 999.99 | 1000 | true | 5 | 0 | 0.99999 | Fail (W-F01) |
| W-0475 | 999.99 | 1000 | true | 6 | 0 | 0.99999 | Fail (W-F01) |
| W-0476 | 999.99 | 1000 | true | 7 | 0.99999 | 0 | Fail (W-F01) |
| W-0477 | 999.99 | 1000 | true | 8 | Unspecified | 0.99999 | Observation |
| W-0478 | 999.99 | 1000.01 | true | 0 | Unspecified | 0.99999 | Observation |
| W-0479 | 999.99 | 1000.01 | true | 1 | 0.99999 | 0 | Fail (W-F01) |
| W-0480 | 999.99 | 1000.01 | true | 2 | 0 | 0.99999 | Fail (W-F01) |
| W-0481 | 999.99 | 1000.01 | true | 3 | 0 | 0.99999 | Fail (W-F01) |
| W-0482 | 999.99 | 1000.01 | true | 4 | 0 | 0.99999 | Fail (W-F01) |
| W-0483 | 999.99 | 1000.01 | true | 5 | 0 | 0.99999 | Fail (W-F01) |
| W-0484 | 999.99 | 1000.01 | true | 6 | 0 | 0.99999 | Fail (W-F01) |
| W-0485 | 999.99 | 1000.01 | true | 7 | 0.99999 | 0 | Fail (W-F01) |
| W-0486 | 999.99 | 1000.01 | true | 8 | Unspecified | 0.99999 | Observation |
| W-0487 | 999.99 | 4999.99 | true | 0 | Unspecified | 0.99999 | Observation |
| W-0488 | 999.99 | 4999.99 | true | 1 | 0.99999 | 0 | Fail (W-F01) |
| W-0489 | 999.99 | 4999.99 | true | 2 | 0 | 0.99999 | Fail (W-F01) |
| W-0490 | 999.99 | 4999.99 | true | 3 | 0 | 0.99999 | Fail (W-F01) |
| W-0491 | 999.99 | 4999.99 | true | 4 | 0 | 0.99999 | Fail (W-F01) |
| W-0492 | 999.99 | 4999.99 | true | 5 | 0 | 0.99999 | Fail (W-F01) |
| W-0493 | 999.99 | 4999.99 | true | 6 | 0 | 0.99999 | Fail (W-F01) |
| W-0494 | 999.99 | 4999.99 | true | 7 | 0.99999 | 0 | Fail (W-F01) |
| W-0495 | 999.99 | 4999.99 | true | 8 | Unspecified | 0.99999 | Observation |
| W-0496 | 999.99 | 5000 | true | 0 | Unspecified | 0.99999 | Observation |
| W-0497 | 999.99 | 5000 | true | 1 | 0.99999 | 0 | Fail (W-F01) |
| W-0498 | 999.99 | 5000 | true | 2 | 0 | 0.99999 | Fail (W-F01) |
| W-0499 | 999.99 | 5000 | true | 3 | 0 | 0.99999 | Fail (W-F01) |
| W-0500 | 999.99 | 5000 | true | 4 | 0 | 0.99999 | Fail (W-F01) |
| W-0501 | 999.99 | 5000 | true | 5 | 0 | 0.99999 | Fail (W-F01) |
| W-0502 | 999.99 | 5000 | true | 6 | 0 | 0.99999 | Fail (W-F01) |
| W-0503 | 999.99 | 5000 | true | 7 | 0.99999 | 0 | Fail (W-F01) |
| W-0504 | 999.99 | 5000 | true | 8 | Unspecified | 0.99999 | Observation |
| W-0505 | 999.99 | 5000.01 | true | 0 | Unspecified | 0.99999 | Observation |
| W-0506 | 999.99 | 5000.01 | true | 1 | 0.99999 | 0 | Fail (W-F01) |
| W-0507 | 999.99 | 5000.01 | true | 2 | 0 | 0.99999 | Fail (W-F01) |
| W-0508 | 999.99 | 5000.01 | true | 3 | 0 | 0.99999 | Fail (W-F01) |
| W-0509 | 999.99 | 5000.01 | true | 4 | 0 | 0.99999 | Fail (W-F01) |
| W-0510 | 999.99 | 5000.01 | true | 5 | 0 | 0.99999 | Fail (W-F01) |
| W-0511 | 999.99 | 5000.01 | true | 6 | 0 | 0.99999 | Fail (W-F01) |
| W-0512 | 999.99 | 5000.01 | true | 7 | 0.99999 | 0 | Fail (W-F01) |
| W-0513 | 999.99 | 5000.01 | true | 8 | Unspecified | 0.99999 | Observation |
| W-0514 | 999.99 | 9999.99 | true | 0 | Unspecified | 0.99999 | Observation |
| W-0515 | 999.99 | 9999.99 | true | 1 | 0.99999 | 0 | Fail (W-F01) |
| W-0516 | 999.99 | 9999.99 | true | 2 | 0 | 0.99999 | Fail (W-F01) |
| W-0517 | 999.99 | 9999.99 | true | 3 | 0 | 0.99999 | Fail (W-F01) |
| W-0518 | 999.99 | 9999.99 | true | 4 | 0 | 0.99999 | Fail (W-F01) |
| W-0519 | 999.99 | 9999.99 | true | 5 | 0 | 0.99999 | Fail (W-F01) |
| W-0520 | 999.99 | 9999.99 | true | 6 | 0 | 0.99999 | Fail (W-F01) |
| W-0521 | 999.99 | 9999.99 | true | 7 | 0.99999 | 0 | Fail (W-F01) |
| W-0522 | 999.99 | 9999.99 | true | 8 | Unspecified | 0.99999 | Observation |
| W-0523 | 999.99 | 10000 | true | 0 | Unspecified | 0.99999 | Observation |
| W-0524 | 999.99 | 10000 | true | 1 | 0.99999 | 0 | Fail (W-F01) |
| W-0525 | 999.99 | 10000 | true | 2 | 0 | 0.99999 | Fail (W-F01) |
| W-0526 | 999.99 | 10000 | true | 3 | 0 | 0.99999 | Fail (W-F01) |
| W-0527 | 999.99 | 10000 | true | 4 | 0 | 0.99999 | Fail (W-F01) |
| W-0528 | 999.99 | 10000 | true | 5 | 0 | 0.99999 | Fail (W-F01) |
| W-0529 | 999.99 | 10000 | true | 6 | 0 | 0.99999 | Fail (W-F01) |
| W-0530 | 999.99 | 10000 | true | 7 | 0.99999 | 0 | Fail (W-F01) |
| W-0531 | 999.99 | 10000 | true | 8 | Unspecified | 0.99999 | Observation |
| W-0532 | 999.99 | 10000.01 | true | 0 | Unspecified | 0.99999 | Observation |
| W-0533 | 999.99 | 10000.01 | true | 1 | 0.99999 | 0 | Fail (W-F01) |
| W-0534 | 999.99 | 10000.01 | true | 2 | 0 | 0.99999 | Fail (W-F01) |
| W-0535 | 999.99 | 10000.01 | true | 3 | 0 | 0.99999 | Fail (W-F01) |
| W-0536 | 999.99 | 10000.01 | true | 4 | 0 | 0.99999 | Fail (W-F01) |
| W-0537 | 999.99 | 10000.01 | true | 5 | 0 | 0.99999 | Fail (W-F01) |
| W-0538 | 999.99 | 10000.01 | true | 6 | 0 | 0.99999 | Fail (W-F01) |
| W-0539 | 999.99 | 10000.01 | true | 7 | 0.99999 | 0 | Fail (W-F01) |
| W-0540 | 999.99 | 10000.01 | true | 8 | Unspecified | 0.99999 | Observation |
| W-0541 | 1000 | -0.01 | true | 0 | Unspecified | 1 | Observation |
| W-0542 | 1000 | -0.01 | true | 1 | Unspecified | 0 | Observation |
| W-0543 | 1000 | -0.01 | true | 2 | Unspecified | 1 | Observation |
| W-0544 | 1000 | -0.01 | true | 3 | Unspecified | 1 | Observation |
| W-0545 | 1000 | -0.01 | true | 4 | Unspecified | 1 | Observation |
| W-0546 | 1000 | -0.01 | true | 5 | Unspecified | 1 | Observation |
| W-0547 | 1000 | -0.01 | true | 6 | Unspecified | 1 | Observation |
| W-0548 | 1000 | -0.01 | true | 7 | Unspecified | 0 | Observation |
| W-0549 | 1000 | -0.01 | true | 8 | Unspecified | 1 | Observation |
| W-0550 | 1000 | 0 | true | 0 | Unspecified | 1 | Observation |
| W-0551 | 1000 | 0 | true | 1 | 1 | 0 | Fail (W-F01) |
| W-0552 | 1000 | 0 | true | 2 | 0 | 1 | Fail (W-F01) |
| W-0553 | 1000 | 0 | true | 3 | 0 | 1 | Fail (W-F01) |
| W-0554 | 1000 | 0 | true | 4 | 0 | 1 | Fail (W-F01) |
| W-0555 | 1000 | 0 | true | 5 | 0 | 1 | Fail (W-F01) |
| W-0556 | 1000 | 0 | true | 6 | 0 | 1 | Fail (W-F01) |
| W-0557 | 1000 | 0 | true | 7 | 1 | 0 | Fail (W-F01) |
| W-0558 | 1000 | 0 | true | 8 | Unspecified | 1 | Observation |
| W-0559 | 1000 | 0.01 | true | 0 | Unspecified | 1 | Observation |
| W-0560 | 1000 | 0.01 | true | 1 | 1 | 0 | Fail (W-F01) |
| W-0561 | 1000 | 0.01 | true | 2 | 0 | 1 | Fail (W-F01) |
| W-0562 | 1000 | 0.01 | true | 3 | 0 | 1 | Fail (W-F01) |
| W-0563 | 1000 | 0.01 | true | 4 | 0 | 1 | Fail (W-F01) |
| W-0564 | 1000 | 0.01 | true | 5 | 0 | 1 | Fail (W-F01) |
| W-0565 | 1000 | 0.01 | true | 6 | 0 | 1 | Fail (W-F01) |
| W-0566 | 1000 | 0.01 | true | 7 | 1 | 0 | Fail (W-F01) |
| W-0567 | 1000 | 0.01 | true | 8 | Unspecified | 1 | Observation |
| W-0568 | 1000 | 999.99 | true | 0 | Unspecified | 1 | Observation |
| W-0569 | 1000 | 999.99 | true | 1 | 1 | 0 | Fail (W-F01) |
| W-0570 | 1000 | 999.99 | true | 2 | 0 | 1 | Fail (W-F01) |
| W-0571 | 1000 | 999.99 | true | 3 | 0 | 1 | Fail (W-F01) |
| W-0572 | 1000 | 999.99 | true | 4 | 0 | 1 | Fail (W-F01) |
| W-0573 | 1000 | 999.99 | true | 5 | 0 | 1 | Fail (W-F01) |
| W-0574 | 1000 | 999.99 | true | 6 | 0 | 1 | Fail (W-F01) |
| W-0575 | 1000 | 999.99 | true | 7 | 1 | 0 | Fail (W-F01) |
| W-0576 | 1000 | 999.99 | true | 8 | Unspecified | 1 | Observation |
| W-0577 | 1000 | 1000 | true | 0 | Unspecified | 1 | Observation |
| W-0578 | 1000 | 1000 | true | 1 | 1 | 0 | Fail (W-F01) |
| W-0579 | 1000 | 1000 | true | 2 | 0 | 1 | Fail (W-F01) |
| W-0580 | 1000 | 1000 | true | 3 | 0 | 1 | Fail (W-F01) |
| W-0581 | 1000 | 1000 | true | 4 | 0 | 1 | Fail (W-F01) |
| W-0582 | 1000 | 1000 | true | 5 | 0 | 1 | Fail (W-F01) |
| W-0583 | 1000 | 1000 | true | 6 | 0 | 1 | Fail (W-F01) |
| W-0584 | 1000 | 1000 | true | 7 | 1 | 0 | Fail (W-F01) |
| W-0585 | 1000 | 1000 | true | 8 | Unspecified | 1 | Observation |
| W-0586 | 1000 | 1000.01 | true | 0 | Unspecified | 1 | Observation |
| W-0587 | 1000 | 1000.01 | true | 1 | 1 | 0 | Fail (W-F01) |
| W-0588 | 1000 | 1000.01 | true | 2 | 0 | 1 | Fail (W-F01) |
| W-0589 | 1000 | 1000.01 | true | 3 | 0 | 1 | Fail (W-F01) |
| W-0590 | 1000 | 1000.01 | true | 4 | 0 | 1 | Fail (W-F01) |
| W-0591 | 1000 | 1000.01 | true | 5 | 0 | 1 | Fail (W-F01) |
| W-0592 | 1000 | 1000.01 | true | 6 | 0 | 1 | Fail (W-F01) |
| W-0593 | 1000 | 1000.01 | true | 7 | 1 | 0 | Fail (W-F01) |
| W-0594 | 1000 | 1000.01 | true | 8 | Unspecified | 1 | Observation |
| W-0595 | 1000 | 4999.99 | true | 0 | Unspecified | 1 | Observation |
| W-0596 | 1000 | 4999.99 | true | 1 | 1 | 0 | Fail (W-F01) |
| W-0597 | 1000 | 4999.99 | true | 2 | 0 | 1 | Fail (W-F01) |
| W-0598 | 1000 | 4999.99 | true | 3 | 0 | 1 | Fail (W-F01) |
| W-0599 | 1000 | 4999.99 | true | 4 | 0 | 1 | Fail (W-F01) |
| W-0600 | 1000 | 4999.99 | true | 5 | 0 | 1 | Fail (W-F01) |
| W-0601 | 1000 | 4999.99 | true | 6 | 0 | 1 | Fail (W-F01) |
| W-0602 | 1000 | 4999.99 | true | 7 | 1 | 0 | Fail (W-F01) |
| W-0603 | 1000 | 4999.99 | true | 8 | Unspecified | 1 | Observation |
| W-0604 | 1000 | 5000 | true | 0 | Unspecified | 1 | Observation |
| W-0605 | 1000 | 5000 | true | 1 | 1 | 0 | Fail (W-F01) |
| W-0606 | 1000 | 5000 | true | 2 | 0 | 1 | Fail (W-F01) |
| W-0607 | 1000 | 5000 | true | 3 | 0 | 1 | Fail (W-F01) |
| W-0608 | 1000 | 5000 | true | 4 | 0 | 1 | Fail (W-F01) |
| W-0609 | 1000 | 5000 | true | 5 | 0 | 1 | Fail (W-F01) |
| W-0610 | 1000 | 5000 | true | 6 | 0 | 1 | Fail (W-F01) |
| W-0611 | 1000 | 5000 | true | 7 | 1 | 0 | Fail (W-F01) |
| W-0612 | 1000 | 5000 | true | 8 | Unspecified | 1 | Observation |
| W-0613 | 1000 | 5000.01 | true | 0 | Unspecified | 1 | Observation |
| W-0614 | 1000 | 5000.01 | true | 1 | 1 | 0 | Fail (W-F01) |
| W-0615 | 1000 | 5000.01 | true | 2 | 0 | 1 | Fail (W-F01) |
| W-0616 | 1000 | 5000.01 | true | 3 | 0 | 1 | Fail (W-F01) |
| W-0617 | 1000 | 5000.01 | true | 4 | 0 | 1 | Fail (W-F01) |
| W-0618 | 1000 | 5000.01 | true | 5 | 0 | 1 | Fail (W-F01) |
| W-0619 | 1000 | 5000.01 | true | 6 | 0 | 1 | Fail (W-F01) |
| W-0620 | 1000 | 5000.01 | true | 7 | 1 | 0 | Fail (W-F01) |
| W-0621 | 1000 | 5000.01 | true | 8 | Unspecified | 1 | Observation |
| W-0622 | 1000 | 9999.99 | true | 0 | Unspecified | 1 | Observation |
| W-0623 | 1000 | 9999.99 | true | 1 | 1 | 0 | Fail (W-F01) |
| W-0624 | 1000 | 9999.99 | true | 2 | 0 | 1 | Fail (W-F01) |
| W-0625 | 1000 | 9999.99 | true | 3 | 0 | 1 | Fail (W-F01) |
| W-0626 | 1000 | 9999.99 | true | 4 | 0 | 1 | Fail (W-F01) |
| W-0627 | 1000 | 9999.99 | true | 5 | 0 | 1 | Fail (W-F01) |
| W-0628 | 1000 | 9999.99 | true | 6 | 0 | 1 | Fail (W-F01) |
| W-0629 | 1000 | 9999.99 | true | 7 | 1 | 0 | Fail (W-F01) |
| W-0630 | 1000 | 9999.99 | true | 8 | Unspecified | 1 | Observation |
| W-0631 | 1000 | 10000 | true | 0 | Unspecified | 1 | Observation |
| W-0632 | 1000 | 10000 | true | 1 | 1 | 0 | Fail (W-F01) |
| W-0633 | 1000 | 10000 | true | 2 | 0 | 1 | Fail (W-F01) |
| W-0634 | 1000 | 10000 | true | 3 | 0 | 1 | Fail (W-F01) |
| W-0635 | 1000 | 10000 | true | 4 | 0 | 1 | Fail (W-F01) |
| W-0636 | 1000 | 10000 | true | 5 | 0 | 1 | Fail (W-F01) |
| W-0637 | 1000 | 10000 | true | 6 | 0 | 1 | Fail (W-F01) |
| W-0638 | 1000 | 10000 | true | 7 | 1 | 0 | Fail (W-F01) |
| W-0639 | 1000 | 10000 | true | 8 | Unspecified | 1 | Observation |
| W-0640 | 1000 | 10000.01 | true | 0 | Unspecified | 1 | Observation |
| W-0641 | 1000 | 10000.01 | true | 1 | 1 | 0 | Fail (W-F01) |
| W-0642 | 1000 | 10000.01 | true | 2 | 0 | 1 | Fail (W-F01) |
| W-0643 | 1000 | 10000.01 | true | 3 | 0 | 1 | Fail (W-F01) |
| W-0644 | 1000 | 10000.01 | true | 4 | 0 | 1 | Fail (W-F01) |
| W-0645 | 1000 | 10000.01 | true | 5 | 0 | 1 | Fail (W-F01) |
| W-0646 | 1000 | 10000.01 | true | 6 | 0 | 1 | Fail (W-F01) |
| W-0647 | 1000 | 10000.01 | true | 7 | 1 | 0 | Fail (W-F01) |
| W-0648 | 1000 | 10000.01 | true | 8 | Unspecified | 1 | Observation |
| W-0649 | 1000.01 | -0.01 | true | 0 | Unspecified | 1.00001 | Observation |
| W-0650 | 1000.01 | -0.01 | true | 1 | Unspecified | 0 | Observation |
| W-0651 | 1000.01 | -0.01 | true | 2 | Unspecified | 1.00001 | Observation |
| W-0652 | 1000.01 | -0.01 | true | 3 | Unspecified | 1.00001 | Observation |
| W-0653 | 1000.01 | -0.01 | true | 4 | Unspecified | 1.00001 | Observation |
| W-0654 | 1000.01 | -0.01 | true | 5 | Unspecified | 1.00001 | Observation |
| W-0655 | 1000.01 | -0.01 | true | 6 | Unspecified | 1.00001 | Observation |
| W-0656 | 1000.01 | -0.01 | true | 7 | Unspecified | 0 | Observation |
| W-0657 | 1000.01 | -0.01 | true | 8 | Unspecified | 1.00001 | Observation |
| W-0658 | 1000.01 | 0 | true | 0 | Unspecified | 1.00001 | Observation |
| W-0659 | 1000.01 | 0 | true | 1 | 1.00001 | 0 | Fail (W-F01) |
| W-0660 | 1000.01 | 0 | true | 2 | 0 | 1.00001 | Fail (W-F01) |
| W-0661 | 1000.01 | 0 | true | 3 | 0 | 1.00001 | Fail (W-F01) |
| W-0662 | 1000.01 | 0 | true | 4 | 0 | 1.00001 | Fail (W-F01) |
| W-0663 | 1000.01 | 0 | true | 5 | 0 | 1.00001 | Fail (W-F01) |
| W-0664 | 1000.01 | 0 | true | 6 | 0 | 1.00001 | Fail (W-F01) |
| W-0665 | 1000.01 | 0 | true | 7 | 1.00001 | 0 | Fail (W-F01) |
| W-0666 | 1000.01 | 0 | true | 8 | Unspecified | 1.00001 | Observation |
| W-0667 | 1000.01 | 0.01 | true | 0 | Unspecified | 1.00001 | Observation |
| W-0668 | 1000.01 | 0.01 | true | 1 | 1.00001 | 0 | Fail (W-F01) |
| W-0669 | 1000.01 | 0.01 | true | 2 | 0 | 1.00001 | Fail (W-F01) |
| W-0670 | 1000.01 | 0.01 | true | 3 | 0 | 1.00001 | Fail (W-F01) |
| W-0671 | 1000.01 | 0.01 | true | 4 | 0 | 1.00001 | Fail (W-F01) |
| W-0672 | 1000.01 | 0.01 | true | 5 | 0 | 1.00001 | Fail (W-F01) |
| W-0673 | 1000.01 | 0.01 | true | 6 | 0 | 1.00001 | Fail (W-F01) |
| W-0674 | 1000.01 | 0.01 | true | 7 | 1.00001 | 0 | Fail (W-F01) |
| W-0675 | 1000.01 | 0.01 | true | 8 | Unspecified | 1.00001 | Observation |
| W-0676 | 1000.01 | 999.99 | true | 0 | Unspecified | 1.00001 | Observation |
| W-0677 | 1000.01 | 999.99 | true | 1 | 1.00001 | 0 | Fail (W-F01) |
| W-0678 | 1000.01 | 999.99 | true | 2 | 0 | 1.00001 | Fail (W-F01) |
| W-0679 | 1000.01 | 999.99 | true | 3 | 0 | 1.00001 | Fail (W-F01) |
| W-0680 | 1000.01 | 999.99 | true | 4 | 0 | 1.00001 | Fail (W-F01) |
| W-0681 | 1000.01 | 999.99 | true | 5 | 0 | 1.00001 | Fail (W-F01) |
| W-0682 | 1000.01 | 999.99 | true | 6 | 0 | 1.00001 | Fail (W-F01) |
| W-0683 | 1000.01 | 999.99 | true | 7 | 1.00001 | 0 | Fail (W-F01) |
| W-0684 | 1000.01 | 999.99 | true | 8 | Unspecified | 1.00001 | Observation |
| W-0685 | 1000.01 | 1000 | true | 0 | Unspecified | 1.00001 | Observation |
| W-0686 | 1000.01 | 1000 | true | 1 | 1.00001 | 0 | Fail (W-F01) |
| W-0687 | 1000.01 | 1000 | true | 2 | 0 | 1.00001 | Fail (W-F01) |
| W-0688 | 1000.01 | 1000 | true | 3 | 0 | 1.00001 | Fail (W-F01) |
| W-0689 | 1000.01 | 1000 | true | 4 | 0 | 1.00001 | Fail (W-F01) |
| W-0690 | 1000.01 | 1000 | true | 5 | 0 | 1.00001 | Fail (W-F01) |
| W-0691 | 1000.01 | 1000 | true | 6 | 0 | 1.00001 | Fail (W-F01) |
| W-0692 | 1000.01 | 1000 | true | 7 | 1.00001 | 0 | Fail (W-F01) |
| W-0693 | 1000.01 | 1000 | true | 8 | Unspecified | 1.00001 | Observation |
| W-0694 | 1000.01 | 1000.01 | true | 0 | Unspecified | 1.00001 | Observation |
| W-0695 | 1000.01 | 1000.01 | true | 1 | 1.00001 | 0 | Fail (W-F01) |
| W-0696 | 1000.01 | 1000.01 | true | 2 | 0 | 1.00001 | Fail (W-F01) |
| W-0697 | 1000.01 | 1000.01 | true | 3 | 0 | 1.00001 | Fail (W-F01) |
| W-0698 | 1000.01 | 1000.01 | true | 4 | 0 | 1.00001 | Fail (W-F01) |
| W-0699 | 1000.01 | 1000.01 | true | 5 | 0 | 1.00001 | Fail (W-F01) |
| W-0700 | 1000.01 | 1000.01 | true | 6 | 0 | 1.00001 | Fail (W-F01) |
| W-0701 | 1000.01 | 1000.01 | true | 7 | 1.00001 | 0 | Fail (W-F01) |
| W-0702 | 1000.01 | 1000.01 | true | 8 | Unspecified | 1.00001 | Observation |
| W-0703 | 1000.01 | 4999.99 | true | 0 | Unspecified | 1.00001 | Observation |
| W-0704 | 1000.01 | 4999.99 | true | 1 | 1.00001 | 0 | Fail (W-F01) |
| W-0705 | 1000.01 | 4999.99 | true | 2 | 0 | 1.00001 | Fail (W-F01) |
| W-0706 | 1000.01 | 4999.99 | true | 3 | 0 | 1.00001 | Fail (W-F01) |
| W-0707 | 1000.01 | 4999.99 | true | 4 | 0 | 1.00001 | Fail (W-F01) |
| W-0708 | 1000.01 | 4999.99 | true | 5 | 0 | 1.00001 | Fail (W-F01) |
| W-0709 | 1000.01 | 4999.99 | true | 6 | 0 | 1.00001 | Fail (W-F01) |
| W-0710 | 1000.01 | 4999.99 | true | 7 | 1.00001 | 0 | Fail (W-F01) |
| W-0711 | 1000.01 | 4999.99 | true | 8 | Unspecified | 1.00001 | Observation |
| W-0712 | 1000.01 | 5000 | true | 0 | Unspecified | 1.00001 | Observation |
| W-0713 | 1000.01 | 5000 | true | 1 | 1.00001 | 0 | Fail (W-F01) |
| W-0714 | 1000.01 | 5000 | true | 2 | 0 | 1.00001 | Fail (W-F01) |
| W-0715 | 1000.01 | 5000 | true | 3 | 0 | 1.00001 | Fail (W-F01) |
| W-0716 | 1000.01 | 5000 | true | 4 | 0 | 1.00001 | Fail (W-F01) |
| W-0717 | 1000.01 | 5000 | true | 5 | 0 | 1.00001 | Fail (W-F01) |
| W-0718 | 1000.01 | 5000 | true | 6 | 0 | 1.00001 | Fail (W-F01) |
| W-0719 | 1000.01 | 5000 | true | 7 | 1.00001 | 0 | Fail (W-F01) |
| W-0720 | 1000.01 | 5000 | true | 8 | Unspecified | 1.00001 | Observation |
| W-0721 | 1000.01 | 5000.01 | true | 0 | Unspecified | 1.00001 | Observation |
| W-0722 | 1000.01 | 5000.01 | true | 1 | 1.00001 | 0 | Fail (W-F01) |
| W-0723 | 1000.01 | 5000.01 | true | 2 | 0 | 1.00001 | Fail (W-F01) |
| W-0724 | 1000.01 | 5000.01 | true | 3 | 0 | 1.00001 | Fail (W-F01) |
| W-0725 | 1000.01 | 5000.01 | true | 4 | 0 | 1.00001 | Fail (W-F01) |
| W-0726 | 1000.01 | 5000.01 | true | 5 | 0 | 1.00001 | Fail (W-F01) |
| W-0727 | 1000.01 | 5000.01 | true | 6 | 0 | 1.00001 | Fail (W-F01) |
| W-0728 | 1000.01 | 5000.01 | true | 7 | 1.00001 | 0 | Fail (W-F01) |
| W-0729 | 1000.01 | 5000.01 | true | 8 | Unspecified | 1.00001 | Observation |
| W-0730 | 1000.01 | 9999.99 | true | 0 | Unspecified | 1.00001 | Observation |
| W-0731 | 1000.01 | 9999.99 | true | 1 | 1.00001 | 0 | Fail (W-F01) |
| W-0732 | 1000.01 | 9999.99 | true | 2 | 0 | 1.00001 | Fail (W-F01) |
| W-0733 | 1000.01 | 9999.99 | true | 3 | 0 | 1.00001 | Fail (W-F01) |
| W-0734 | 1000.01 | 9999.99 | true | 4 | 0 | 1.00001 | Fail (W-F01) |
| W-0735 | 1000.01 | 9999.99 | true | 5 | 0 | 1.00001 | Fail (W-F01) |
| W-0736 | 1000.01 | 9999.99 | true | 6 | 0 | 1.00001 | Fail (W-F01) |
| W-0737 | 1000.01 | 9999.99 | true | 7 | 1.00001 | 0 | Fail (W-F01) |
| W-0738 | 1000.01 | 9999.99 | true | 8 | Unspecified | 1.00001 | Observation |
| W-0739 | 1000.01 | 10000 | true | 0 | Unspecified | 1.00001 | Observation |
| W-0740 | 1000.01 | 10000 | true | 1 | 1.00001 | 0 | Fail (W-F01) |
| W-0741 | 1000.01 | 10000 | true | 2 | 0 | 1.00001 | Fail (W-F01) |
| W-0742 | 1000.01 | 10000 | true | 3 | 0 | 1.00001 | Fail (W-F01) |
| W-0743 | 1000.01 | 10000 | true | 4 | 0 | 1.00001 | Fail (W-F01) |
| W-0744 | 1000.01 | 10000 | true | 5 | 0 | 1.00001 | Fail (W-F01) |
| W-0745 | 1000.01 | 10000 | true | 6 | 0 | 1.00001 | Fail (W-F01) |
| W-0746 | 1000.01 | 10000 | true | 7 | 1.00001 | 0 | Fail (W-F01) |
| W-0747 | 1000.01 | 10000 | true | 8 | Unspecified | 1.00001 | Observation |
| W-0748 | 1000.01 | 10000.01 | true | 0 | Unspecified | 1.00001 | Observation |
| W-0749 | 1000.01 | 10000.01 | true | 1 | 1.00001 | 0 | Fail (W-F01) |
| W-0750 | 1000.01 | 10000.01 | true | 2 | 0 | 1.00001 | Fail (W-F01) |
| W-0751 | 1000.01 | 10000.01 | true | 3 | 0 | 1.00001 | Fail (W-F01) |
| W-0752 | 1000.01 | 10000.01 | true | 4 | 0 | 1.00001 | Fail (W-F01) |
| W-0753 | 1000.01 | 10000.01 | true | 5 | 0 | 1.00001 | Fail (W-F01) |
| W-0754 | 1000.01 | 10000.01 | true | 6 | 0 | 1.00001 | Fail (W-F01) |
| W-0755 | 1000.01 | 10000.01 | true | 7 | 1.00001 | 0 | Fail (W-F01) |
| W-0756 | 1000.01 | 10000.01 | true | 8 | Unspecified | 1.00001 | Observation |
| W-0757 | -0.01 | -0.01 | false | 0 | Unspecified | -0.00002 | Observation |
| W-0758 | -0.01 | -0.01 | false | 1 | Unspecified | -0.00002 | Observation |
| W-0759 | -0.01 | -0.01 | false | 2 | Unspecified | -0.00002 | Observation |
| W-0760 | -0.01 | -0.01 | false | 3 | Unspecified | -0.00002 | Observation |
| W-0761 | -0.01 | -0.01 | false | 4 | Unspecified | -0.00002 | Observation |
| W-0762 | -0.01 | -0.01 | false | 5 | Unspecified | -0.00002 | Observation |
| W-0763 | -0.01 | -0.01 | false | 6 | Unspecified | -0.00002 | Observation |
| W-0764 | -0.01 | -0.01 | false | 7 | Unspecified | -0.00002 | Observation |
| W-0765 | -0.01 | -0.01 | false | 8 | Unspecified | -0.00002 | Observation |
| W-0766 | -0.01 | 0 | false | 0 | Unspecified | -0.00002 | Observation |
| W-0767 | -0.01 | 0 | false | 1 | Unspecified | -0.00002 | Observation |
| W-0768 | -0.01 | 0 | false | 2 | Unspecified | -0.00002 | Observation |
| W-0769 | -0.01 | 0 | false | 3 | Unspecified | -0.00002 | Observation |
| W-0770 | -0.01 | 0 | false | 4 | Unspecified | -0.00002 | Observation |
| W-0771 | -0.01 | 0 | false | 5 | Unspecified | -0.00002 | Observation |
| W-0772 | -0.01 | 0 | false | 6 | Unspecified | -0.00002 | Observation |
| W-0773 | -0.01 | 0 | false | 7 | Unspecified | -0.00002 | Observation |
| W-0774 | -0.01 | 0 | false | 8 | Unspecified | -0.00002 | Observation |
| W-0775 | -0.01 | 0.01 | false | 0 | Unspecified | -0.00002 | Observation |
| W-0776 | -0.01 | 0.01 | false | 1 | Unspecified | -0.00002 | Observation |
| W-0777 | -0.01 | 0.01 | false | 2 | Unspecified | -0.00002 | Observation |
| W-0778 | -0.01 | 0.01 | false | 3 | Unspecified | -0.00002 | Observation |
| W-0779 | -0.01 | 0.01 | false | 4 | Unspecified | -0.00002 | Observation |
| W-0780 | -0.01 | 0.01 | false | 5 | Unspecified | -0.00002 | Observation |
| W-0781 | -0.01 | 0.01 | false | 6 | Unspecified | -0.00002 | Observation |
| W-0782 | -0.01 | 0.01 | false | 7 | Unspecified | -0.00002 | Observation |
| W-0783 | -0.01 | 0.01 | false | 8 | Unspecified | -0.00002 | Observation |
| W-0784 | -0.01 | 999.99 | false | 0 | Unspecified | -0.00002 | Observation |
| W-0785 | -0.01 | 999.99 | false | 1 | Unspecified | -0.00002 | Observation |
| W-0786 | -0.01 | 999.99 | false | 2 | Unspecified | -0.00002 | Observation |
| W-0787 | -0.01 | 999.99 | false | 3 | Unspecified | -0.00002 | Observation |
| W-0788 | -0.01 | 999.99 | false | 4 | Unspecified | -0.00002 | Observation |
| W-0789 | -0.01 | 999.99 | false | 5 | Unspecified | -0.00002 | Observation |
| W-0790 | -0.01 | 999.99 | false | 6 | Unspecified | -0.00002 | Observation |
| W-0791 | -0.01 | 999.99 | false | 7 | Unspecified | -0.00002 | Observation |
| W-0792 | -0.01 | 999.99 | false | 8 | Unspecified | -0.00002 | Observation |
| W-0793 | -0.01 | 1000 | false | 0 | Unspecified | -0.00001 | Observation |
| W-0794 | -0.01 | 1000 | false | 1 | Unspecified | -0.00001 | Observation |
| W-0795 | -0.01 | 1000 | false | 2 | Unspecified | -0.00001 | Observation |
| W-0796 | -0.01 | 1000 | false | 3 | Unspecified | -0.00001 | Observation |
| W-0797 | -0.01 | 1000 | false | 4 | Unspecified | -0.00001 | Observation |
| W-0798 | -0.01 | 1000 | false | 5 | Unspecified | -0.00001 | Observation |
| W-0799 | -0.01 | 1000 | false | 6 | Unspecified | -0.00001 | Observation |
| W-0800 | -0.01 | 1000 | false | 7 | Unspecified | -0.00001 | Observation |
| W-0801 | -0.01 | 1000 | false | 8 | Unspecified | -0.00001 | Observation |
| W-0802 | -0.01 | 1000.01 | false | 0 | Unspecified | -0.00001 | Observation |
| W-0803 | -0.01 | 1000.01 | false | 1 | Unspecified | -0.00001 | Observation |
| W-0804 | -0.01 | 1000.01 | false | 2 | Unspecified | -0.00001 | Observation |
| W-0805 | -0.01 | 1000.01 | false | 3 | Unspecified | -0.00001 | Observation |
| W-0806 | -0.01 | 1000.01 | false | 4 | Unspecified | -0.00001 | Observation |
| W-0807 | -0.01 | 1000.01 | false | 5 | Unspecified | -0.00001 | Observation |
| W-0808 | -0.01 | 1000.01 | false | 6 | Unspecified | -0.00001 | Observation |
| W-0809 | -0.01 | 1000.01 | false | 7 | Unspecified | -0.00001 | Observation |
| W-0810 | -0.01 | 1000.01 | false | 8 | Unspecified | -0.00001 | Observation |
| W-0811 | -0.01 | 4999.99 | false | 0 | Unspecified | -0.00001 | Observation |
| W-0812 | -0.01 | 4999.99 | false | 1 | Unspecified | -0.00001 | Observation |
| W-0813 | -0.01 | 4999.99 | false | 2 | Unspecified | -0.00001 | Observation |
| W-0814 | -0.01 | 4999.99 | false | 3 | Unspecified | -0.00001 | Observation |
| W-0815 | -0.01 | 4999.99 | false | 4 | Unspecified | -0.00001 | Observation |
| W-0816 | -0.01 | 4999.99 | false | 5 | Unspecified | -0.00001 | Observation |
| W-0817 | -0.01 | 4999.99 | false | 6 | Unspecified | -0.00001 | Observation |
| W-0818 | -0.01 | 4999.99 | false | 7 | Unspecified | -0.00001 | Observation |
| W-0819 | -0.01 | 4999.99 | false | 8 | Unspecified | -0.00001 | Observation |
| W-0820 | -0.01 | 5000 | false | 0 | Unspecified | -0.00001 | Observation |
| W-0821 | -0.01 | 5000 | false | 1 | Unspecified | -0.00001 | Observation |
| W-0822 | -0.01 | 5000 | false | 2 | Unspecified | -0.00001 | Observation |
| W-0823 | -0.01 | 5000 | false | 3 | Unspecified | -0.00001 | Observation |
| W-0824 | -0.01 | 5000 | false | 4 | Unspecified | -0.00001 | Observation |
| W-0825 | -0.01 | 5000 | false | 5 | Unspecified | -0.00001 | Observation |
| W-0826 | -0.01 | 5000 | false | 6 | Unspecified | -0.00001 | Observation |
| W-0827 | -0.01 | 5000 | false | 7 | Unspecified | -0.00001 | Observation |
| W-0828 | -0.01 | 5000 | false | 8 | Unspecified | -0.00001 | Observation |
| W-0829 | -0.01 | 5000.01 | false | 0 | Unspecified | -0.00001 | Observation |
| W-0830 | -0.01 | 5000.01 | false | 1 | Unspecified | -0.00001 | Observation |
| W-0831 | -0.01 | 5000.01 | false | 2 | Unspecified | -0.00001 | Observation |
| W-0832 | -0.01 | 5000.01 | false | 3 | Unspecified | -0.00001 | Observation |
| W-0833 | -0.01 | 5000.01 | false | 4 | Unspecified | -0.00001 | Observation |
| W-0834 | -0.01 | 5000.01 | false | 5 | Unspecified | -0.00001 | Observation |
| W-0835 | -0.01 | 5000.01 | false | 6 | Unspecified | -0.00001 | Observation |
| W-0836 | -0.01 | 5000.01 | false | 7 | Unspecified | -0.00001 | Observation |
| W-0837 | -0.01 | 5000.01 | false | 8 | Unspecified | -0.00001 | Observation |
| W-0838 | -0.01 | 9999.99 | false | 0 | Unspecified | -0.00001 | Observation |
| W-0839 | -0.01 | 9999.99 | false | 1 | Unspecified | -0.00001 | Observation |
| W-0840 | -0.01 | 9999.99 | false | 2 | Unspecified | -0.00001 | Observation |
| W-0841 | -0.01 | 9999.99 | false | 3 | Unspecified | -0.00001 | Observation |
| W-0842 | -0.01 | 9999.99 | false | 4 | Unspecified | -0.00001 | Observation |
| W-0843 | -0.01 | 9999.99 | false | 5 | Unspecified | -0.00001 | Observation |
| W-0844 | -0.01 | 9999.99 | false | 6 | Unspecified | -0.00001 | Observation |
| W-0845 | -0.01 | 9999.99 | false | 7 | Unspecified | -0.00001 | Observation |
| W-0846 | -0.01 | 9999.99 | false | 8 | Unspecified | -0.00001 | Observation |
| W-0847 | -0.01 | 10000 | false | 0 | Unspecified | -0 | Observation |
| W-0848 | -0.01 | 10000 | false | 1 | Unspecified | -0 | Observation |
| W-0849 | -0.01 | 10000 | false | 2 | Unspecified | -0 | Observation |
| W-0850 | -0.01 | 10000 | false | 3 | Unspecified | -0 | Observation |
| W-0851 | -0.01 | 10000 | false | 4 | Unspecified | -0 | Observation |
| W-0852 | -0.01 | 10000 | false | 5 | Unspecified | -0 | Observation |
| W-0853 | -0.01 | 10000 | false | 6 | Unspecified | -0 | Observation |
| W-0854 | -0.01 | 10000 | false | 7 | Unspecified | -0 | Observation |
| W-0855 | -0.01 | 10000 | false | 8 | Unspecified | -0 | Observation |
| W-0856 | -0.01 | 10000.01 | false | 0 | Unspecified | -0 | Observation |
| W-0857 | -0.01 | 10000.01 | false | 1 | Unspecified | -0 | Observation |
| W-0858 | -0.01 | 10000.01 | false | 2 | Unspecified | -0 | Observation |
| W-0859 | -0.01 | 10000.01 | false | 3 | Unspecified | -0 | Observation |
| W-0860 | -0.01 | 10000.01 | false | 4 | Unspecified | -0 | Observation |
| W-0861 | -0.01 | 10000.01 | false | 5 | Unspecified | -0 | Observation |
| W-0862 | -0.01 | 10000.01 | false | 6 | Unspecified | -0 | Observation |
| W-0863 | -0.01 | 10000.01 | false | 7 | Unspecified | -0 | Observation |
| W-0864 | -0.01 | 10000.01 | false | 8 | Unspecified | -0 | Observation |
| W-0865 | 0 | -0.01 | false | 0 | Unspecified | 0 | Observation |
| W-0866 | 0 | -0.01 | false | 1 | Unspecified | 0 | Observation |
| W-0867 | 0 | -0.01 | false | 2 | Unspecified | 0 | Observation |
| W-0868 | 0 | -0.01 | false | 3 | Unspecified | 0 | Observation |
| W-0869 | 0 | -0.01 | false | 4 | Unspecified | 0 | Observation |
| W-0870 | 0 | -0.01 | false | 5 | Unspecified | 0 | Observation |
| W-0871 | 0 | -0.01 | false | 6 | Unspecified | 0 | Observation |
| W-0872 | 0 | -0.01 | false | 7 | Unspecified | 0 | Observation |
| W-0873 | 0 | -0.01 | false | 8 | Unspecified | 0 | Observation |
| W-0874 | 0 | 0 | false | 0 | Unspecified | 0 | Observation |
| W-0875 | 0 | 0 | false | 1 | 0 | Matches expected | Pass |
| W-0876 | 0 | 0 | false | 2 | 0 | Matches expected | Pass |
| W-0877 | 0 | 0 | false | 3 | 0 | Matches expected | Pass |
| W-0878 | 0 | 0 | false | 4 | 0 | Matches expected | Pass |
| W-0879 | 0 | 0 | false | 5 | 0 | Matches expected | Pass |
| W-0880 | 0 | 0 | false | 6 | 0 | Matches expected | Pass |
| W-0881 | 0 | 0 | false | 7 | 0 | Matches expected | Pass |
| W-0882 | 0 | 0 | false | 8 | Unspecified | 0 | Observation |
| W-0883 | 0 | 0.01 | false | 0 | Unspecified | 0 | Observation |
| W-0884 | 0 | 0.01 | false | 1 | 0 | Matches expected | Pass |
| W-0885 | 0 | 0.01 | false | 2 | 0 | Matches expected | Pass |
| W-0886 | 0 | 0.01 | false | 3 | 0 | Matches expected | Pass |
| W-0887 | 0 | 0.01 | false | 4 | 0 | Matches expected | Pass |
| W-0888 | 0 | 0.01 | false | 5 | 0 | Matches expected | Pass |
| W-0889 | 0 | 0.01 | false | 6 | 0 | Matches expected | Pass |
| W-0890 | 0 | 0.01 | false | 7 | 0 | Matches expected | Pass |
| W-0891 | 0 | 0.01 | false | 8 | Unspecified | 0 | Observation |
| W-0892 | 0 | 999.99 | false | 0 | Unspecified | 0 | Observation |
| W-0893 | 0 | 999.99 | false | 1 | 0 | Matches expected | Pass |
| W-0894 | 0 | 999.99 | false | 2 | 0 | Matches expected | Pass |
| W-0895 | 0 | 999.99 | false | 3 | 0 | Matches expected | Pass |
| W-0896 | 0 | 999.99 | false | 4 | 0 | Matches expected | Pass |
| W-0897 | 0 | 999.99 | false | 5 | 0 | Matches expected | Pass |
| W-0898 | 0 | 999.99 | false | 6 | 0 | Matches expected | Pass |
| W-0899 | 0 | 999.99 | false | 7 | 0 | Matches expected | Pass |
| W-0900 | 0 | 999.99 | false | 8 | Unspecified | 0 | Observation |
| W-0901 | 0 | 1000 | false | 0 | Unspecified | 0 | Observation |
| W-0902 | 0 | 1000 | false | 1 | 0 | Matches expected | Pass |
| W-0903 | 0 | 1000 | false | 2 | 0 | Matches expected | Pass |
| W-0904 | 0 | 1000 | false | 3 | 0 | Matches expected | Pass |
| W-0905 | 0 | 1000 | false | 4 | 0 | Matches expected | Pass |
| W-0906 | 0 | 1000 | false | 5 | 0 | Matches expected | Pass |
| W-0907 | 0 | 1000 | false | 6 | 0 | Matches expected | Pass |
| W-0908 | 0 | 1000 | false | 7 | 0 | Matches expected | Pass |
| W-0909 | 0 | 1000 | false | 8 | Unspecified | 0 | Observation |
| W-0910 | 0 | 1000.01 | false | 0 | Unspecified | 0 | Observation |
| W-0911 | 0 | 1000.01 | false | 1 | 0 | Matches expected | Pass |
| W-0912 | 0 | 1000.01 | false | 2 | 0 | Matches expected | Pass |
| W-0913 | 0 | 1000.01 | false | 3 | 0 | Matches expected | Pass |
| W-0914 | 0 | 1000.01 | false | 4 | 0 | Matches expected | Pass |
| W-0915 | 0 | 1000.01 | false | 5 | 0 | Matches expected | Pass |
| W-0916 | 0 | 1000.01 | false | 6 | 0 | Matches expected | Pass |
| W-0917 | 0 | 1000.01 | false | 7 | 0 | Matches expected | Pass |
| W-0918 | 0 | 1000.01 | false | 8 | Unspecified | 0 | Observation |
| W-0919 | 0 | 4999.99 | false | 0 | Unspecified | 0 | Observation |
| W-0920 | 0 | 4999.99 | false | 1 | 0 | Matches expected | Pass |
| W-0921 | 0 | 4999.99 | false | 2 | 0 | Matches expected | Pass |
| W-0922 | 0 | 4999.99 | false | 3 | 0 | Matches expected | Pass |
| W-0923 | 0 | 4999.99 | false | 4 | 0 | Matches expected | Pass |
| W-0924 | 0 | 4999.99 | false | 5 | 0 | Matches expected | Pass |
| W-0925 | 0 | 4999.99 | false | 6 | 0 | Matches expected | Pass |
| W-0926 | 0 | 4999.99 | false | 7 | 0 | Matches expected | Pass |
| W-0927 | 0 | 4999.99 | false | 8 | Unspecified | 0 | Observation |
| W-0928 | 0 | 5000 | false | 0 | Unspecified | 0 | Observation |
| W-0929 | 0 | 5000 | false | 1 | 0 | Matches expected | Pass |
| W-0930 | 0 | 5000 | false | 2 | 0 | Matches expected | Pass |
| W-0931 | 0 | 5000 | false | 3 | 0 | Matches expected | Pass |
| W-0932 | 0 | 5000 | false | 4 | 0 | Matches expected | Pass |
| W-0933 | 0 | 5000 | false | 5 | 0 | Matches expected | Pass |
| W-0934 | 0 | 5000 | false | 6 | 0 | Matches expected | Pass |
| W-0935 | 0 | 5000 | false | 7 | 0 | Matches expected | Pass |
| W-0936 | 0 | 5000 | false | 8 | Unspecified | 0 | Observation |
| W-0937 | 0 | 5000.01 | false | 0 | Unspecified | 0 | Observation |
| W-0938 | 0 | 5000.01 | false | 1 | 0 | Matches expected | Pass |
| W-0939 | 0 | 5000.01 | false | 2 | 0 | Matches expected | Pass |
| W-0940 | 0 | 5000.01 | false | 3 | 0 | Matches expected | Pass |
| W-0941 | 0 | 5000.01 | false | 4 | 0 | Matches expected | Pass |
| W-0942 | 0 | 5000.01 | false | 5 | 0 | Matches expected | Pass |
| W-0943 | 0 | 5000.01 | false | 6 | 0 | Matches expected | Pass |
| W-0944 | 0 | 5000.01 | false | 7 | 0 | Matches expected | Pass |
| W-0945 | 0 | 5000.01 | false | 8 | Unspecified | 0 | Observation |
| W-0946 | 0 | 9999.99 | false | 0 | Unspecified | 0 | Observation |
| W-0947 | 0 | 9999.99 | false | 1 | 0 | Matches expected | Pass |
| W-0948 | 0 | 9999.99 | false | 2 | 0 | Matches expected | Pass |
| W-0949 | 0 | 9999.99 | false | 3 | 0 | Matches expected | Pass |
| W-0950 | 0 | 9999.99 | false | 4 | 0 | Matches expected | Pass |
| W-0951 | 0 | 9999.99 | false | 5 | 0 | Matches expected | Pass |
| W-0952 | 0 | 9999.99 | false | 6 | 0 | Matches expected | Pass |
| W-0953 | 0 | 9999.99 | false | 7 | 0 | Matches expected | Pass |
| W-0954 | 0 | 9999.99 | false | 8 | Unspecified | 0 | Observation |
| W-0955 | 0 | 10000 | false | 0 | Unspecified | 0 | Observation |
| W-0956 | 0 | 10000 | false | 1 | 0 | Matches expected | Pass |
| W-0957 | 0 | 10000 | false | 2 | 0 | Matches expected | Pass |
| W-0958 | 0 | 10000 | false | 3 | 0 | Matches expected | Pass |
| W-0959 | 0 | 10000 | false | 4 | 0 | Matches expected | Pass |
| W-0960 | 0 | 10000 | false | 5 | 0 | Matches expected | Pass |
| W-0961 | 0 | 10000 | false | 6 | 0 | Matches expected | Pass |
| W-0962 | 0 | 10000 | false | 7 | 0 | Matches expected | Pass |
| W-0963 | 0 | 10000 | false | 8 | Unspecified | 0 | Observation |
| W-0964 | 0 | 10000.01 | false | 0 | Unspecified | 0 | Observation |
| W-0965 | 0 | 10000.01 | false | 1 | 0 | Matches expected | Pass |
| W-0966 | 0 | 10000.01 | false | 2 | 0 | Matches expected | Pass |
| W-0967 | 0 | 10000.01 | false | 3 | 0 | Matches expected | Pass |
| W-0968 | 0 | 10000.01 | false | 4 | 0 | Matches expected | Pass |
| W-0969 | 0 | 10000.01 | false | 5 | 0 | Matches expected | Pass |
| W-0970 | 0 | 10000.01 | false | 6 | 0 | Matches expected | Pass |
| W-0971 | 0 | 10000.01 | false | 7 | 0 | Matches expected | Pass |
| W-0972 | 0 | 10000.01 | false | 8 | Unspecified | 0 | Observation |
| W-0973 | 0.01 | -0.01 | false | 0 | Unspecified | 0.00002 | Observation |
| W-0974 | 0.01 | -0.01 | false | 1 | Unspecified | 0.00002 | Observation |
| W-0975 | 0.01 | -0.01 | false | 2 | Unspecified | 0.00002 | Observation |
| W-0976 | 0.01 | -0.01 | false | 3 | Unspecified | 0.00002 | Observation |
| W-0977 | 0.01 | -0.01 | false | 4 | Unspecified | 0.00002 | Observation |
| W-0978 | 0.01 | -0.01 | false | 5 | Unspecified | 0.00002 | Observation |
| W-0979 | 0.01 | -0.01 | false | 6 | Unspecified | 0.00002 | Observation |
| W-0980 | 0.01 | -0.01 | false | 7 | Unspecified | 0.00002 | Observation |
| W-0981 | 0.01 | -0.01 | false | 8 | Unspecified | 0.00002 | Observation |
| W-0982 | 0.01 | 0 | false | 0 | Unspecified | 0.00002 | Observation |
| W-0983 | 0.01 | 0 | false | 1 | 0.00003 | 0.00002 | Fail (W-F02) |
| W-0984 | 0.01 | 0 | false | 2 | 0.00003 | 0.00002 | Fail (W-F02) |
| W-0985 | 0.01 | 0 | false | 3 | 0.00003 | 0.00002 | Fail (W-F02) |
| W-0986 | 0.01 | 0 | false | 4 | 0.00003 | 0.00002 | Fail (W-F02) |
| W-0987 | 0.01 | 0 | false | 5 | 0.00003 | 0.00002 | Fail (W-F02) |
| W-0988 | 0.01 | 0 | false | 6 | 0.00003 | 0.00002 | Fail (W-F02) |
| W-0989 | 0.01 | 0 | false | 7 | 0.00003 | 0.00002 | Fail (W-F02) |
| W-0990 | 0.01 | 0 | false | 8 | Unspecified | 0.00002 | Observation |
| W-0991 | 0.01 | 0.01 | false | 0 | Unspecified | 0.00002 | Observation |
| W-0992 | 0.01 | 0.01 | false | 1 | 0.00003 | 0.00002 | Fail (W-F02) |
| W-0993 | 0.01 | 0.01 | false | 2 | 0.00003 | 0.00002 | Fail (W-F02) |
| W-0994 | 0.01 | 0.01 | false | 3 | 0.00003 | 0.00002 | Fail (W-F02) |
| W-0995 | 0.01 | 0.01 | false | 4 | 0.00003 | 0.00002 | Fail (W-F02) |
| W-0996 | 0.01 | 0.01 | false | 5 | 0.00003 | 0.00002 | Fail (W-F02) |
| W-0997 | 0.01 | 0.01 | false | 6 | 0.00003 | 0.00002 | Fail (W-F02) |
| W-0998 | 0.01 | 0.01 | false | 7 | 0.00003 | 0.00002 | Fail (W-F02) |
| W-0999 | 0.01 | 0.01 | false | 8 | Unspecified | 0.00002 | Observation |
| W-1000 | 0.01 | 999.99 | false | 0 | Unspecified | 0.00002 | Observation |
| W-1001 | 0.01 | 999.99 | false | 1 | 0.00003 | 0.00002 | Fail (W-F02) |
| W-1002 | 0.01 | 999.99 | false | 2 | 0.00003 | 0.00002 | Fail (W-F02) |
| W-1003 | 0.01 | 999.99 | false | 3 | 0.00003 | 0.00002 | Fail (W-F02) |
| W-1004 | 0.01 | 999.99 | false | 4 | 0.00003 | 0.00002 | Fail (W-F02) |
| W-1005 | 0.01 | 999.99 | false | 5 | 0.00003 | 0.00002 | Fail (W-F02) |
| W-1006 | 0.01 | 999.99 | false | 6 | 0.00003 | 0.00002 | Fail (W-F02) |
| W-1007 | 0.01 | 999.99 | false | 7 | 0.00003 | 0.00002 | Fail (W-F02) |
| W-1008 | 0.01 | 999.99 | false | 8 | Unspecified | 0.00002 | Observation |
| W-1009 | 0.01 | 1000 | false | 0 | Unspecified | 0.00001 | Observation |
| W-1010 | 0.01 | 1000 | false | 1 | 0.00001 | Matches expected | Pass |
| W-1011 | 0.01 | 1000 | false | 2 | 0.00001 | Matches expected | Pass |
| W-1012 | 0.01 | 1000 | false | 3 | 0.00001 | Matches expected | Pass |
| W-1013 | 0.01 | 1000 | false | 4 | 0.00001 | Matches expected | Pass |
| W-1014 | 0.01 | 1000 | false | 5 | 0.00001 | Matches expected | Pass |
| W-1015 | 0.01 | 1000 | false | 6 | 0.00001 | Matches expected | Pass |
| W-1016 | 0.01 | 1000 | false | 7 | 0.00001 | Matches expected | Pass |
| W-1017 | 0.01 | 1000 | false | 8 | Unspecified | 0.00001 | Observation |
| W-1018 | 0.01 | 1000.01 | false | 0 | Unspecified | 0.00001 | Observation |
| W-1019 | 0.01 | 1000.01 | false | 1 | 0.00001 | Matches expected | Pass |
| W-1020 | 0.01 | 1000.01 | false | 2 | 0.00001 | Matches expected | Pass |
| W-1021 | 0.01 | 1000.01 | false | 3 | 0.00001 | Matches expected | Pass |
| W-1022 | 0.01 | 1000.01 | false | 4 | 0.00001 | Matches expected | Pass |
| W-1023 | 0.01 | 1000.01 | false | 5 | 0.00001 | Matches expected | Pass |
| W-1024 | 0.01 | 1000.01 | false | 6 | 0.00001 | Matches expected | Pass |
| W-1025 | 0.01 | 1000.01 | false | 7 | 0.00001 | Matches expected | Pass |
| W-1026 | 0.01 | 1000.01 | false | 8 | Unspecified | 0.00001 | Observation |
| W-1027 | 0.01 | 4999.99 | false | 0 | Unspecified | 0.00001 | Observation |
| W-1028 | 0.01 | 4999.99 | false | 1 | 0.00001 | Matches expected | Pass |
| W-1029 | 0.01 | 4999.99 | false | 2 | 0.00001 | Matches expected | Pass |
| W-1030 | 0.01 | 4999.99 | false | 3 | 0.00001 | Matches expected | Pass |
| W-1031 | 0.01 | 4999.99 | false | 4 | 0.00001 | Matches expected | Pass |
| W-1032 | 0.01 | 4999.99 | false | 5 | 0.00001 | Matches expected | Pass |
| W-1033 | 0.01 | 4999.99 | false | 6 | 0.00001 | Matches expected | Pass |
| W-1034 | 0.01 | 4999.99 | false | 7 | 0.00001 | Matches expected | Pass |
| W-1035 | 0.01 | 4999.99 | false | 8 | Unspecified | 0.00001 | Observation |
| W-1036 | 0.01 | 5000 | false | 0 | Unspecified | 0.00001 | Observation |
| W-1037 | 0.01 | 5000 | false | 1 | 0 | 0.00001 | Fail (W-F03) |
| W-1038 | 0.01 | 5000 | false | 2 | 0 | 0.00001 | Fail (W-F03) |
| W-1039 | 0.01 | 5000 | false | 3 | 0 | 0.00001 | Fail (W-F03) |
| W-1040 | 0.01 | 5000 | false | 4 | 0 | 0.00001 | Fail (W-F03) |
| W-1041 | 0.01 | 5000 | false | 5 | 0 | 0.00001 | Fail (W-F03) |
| W-1042 | 0.01 | 5000 | false | 6 | 0 | 0.00001 | Fail (W-F03) |
| W-1043 | 0.01 | 5000 | false | 7 | 0 | 0.00001 | Fail (W-F03) |
| W-1044 | 0.01 | 5000 | false | 8 | Unspecified | 0.00001 | Observation |
| W-1045 | 0.01 | 5000.01 | false | 0 | Unspecified | 0.00001 | Observation |
| W-1046 | 0.01 | 5000.01 | false | 1 | 0 | 0.00001 | Fail (W-F03) |
| W-1047 | 0.01 | 5000.01 | false | 2 | 0 | 0.00001 | Fail (W-F03) |
| W-1048 | 0.01 | 5000.01 | false | 3 | 0 | 0.00001 | Fail (W-F03) |
| W-1049 | 0.01 | 5000.01 | false | 4 | 0 | 0.00001 | Fail (W-F03) |
| W-1050 | 0.01 | 5000.01 | false | 5 | 0 | 0.00001 | Fail (W-F03) |
| W-1051 | 0.01 | 5000.01 | false | 6 | 0 | 0.00001 | Fail (W-F03) |
| W-1052 | 0.01 | 5000.01 | false | 7 | 0 | 0.00001 | Fail (W-F03) |
| W-1053 | 0.01 | 5000.01 | false | 8 | Unspecified | 0.00001 | Observation |
| W-1054 | 0.01 | 9999.99 | false | 0 | Unspecified | 0.00001 | Observation |
| W-1055 | 0.01 | 9999.99 | false | 1 | 0 | 0.00001 | Fail (W-F03) |
| W-1056 | 0.01 | 9999.99 | false | 2 | 0 | 0.00001 | Fail (W-F03) |
| W-1057 | 0.01 | 9999.99 | false | 3 | 0 | 0.00001 | Fail (W-F03) |
| W-1058 | 0.01 | 9999.99 | false | 4 | 0 | 0.00001 | Fail (W-F03) |
| W-1059 | 0.01 | 9999.99 | false | 5 | 0 | 0.00001 | Fail (W-F03) |
| W-1060 | 0.01 | 9999.99 | false | 6 | 0 | 0.00001 | Fail (W-F03) |
| W-1061 | 0.01 | 9999.99 | false | 7 | 0 | 0.00001 | Fail (W-F03) |
| W-1062 | 0.01 | 9999.99 | false | 8 | Unspecified | 0.00001 | Observation |
| W-1063 | 0.01 | 10000 | false | 0 | Unspecified | 0 | Observation |
| W-1064 | 0.01 | 10000 | false | 1 | 0 | Matches expected | Pass |
| W-1065 | 0.01 | 10000 | false | 2 | 0 | Matches expected | Pass |
| W-1066 | 0.01 | 10000 | false | 3 | 0 | Matches expected | Pass |
| W-1067 | 0.01 | 10000 | false | 4 | 0 | Matches expected | Pass |
| W-1068 | 0.01 | 10000 | false | 5 | 0 | Matches expected | Pass |
| W-1069 | 0.01 | 10000 | false | 6 | 0 | Matches expected | Pass |
| W-1070 | 0.01 | 10000 | false | 7 | 0 | Matches expected | Pass |
| W-1071 | 0.01 | 10000 | false | 8 | Unspecified | 0 | Observation |
| W-1072 | 0.01 | 10000.01 | false | 0 | Unspecified | 0 | Observation |
| W-1073 | 0.01 | 10000.01 | false | 1 | 0 | Matches expected | Pass |
| W-1074 | 0.01 | 10000.01 | false | 2 | 0 | Matches expected | Pass |
| W-1075 | 0.01 | 10000.01 | false | 3 | 0 | Matches expected | Pass |
| W-1076 | 0.01 | 10000.01 | false | 4 | 0 | Matches expected | Pass |
| W-1077 | 0.01 | 10000.01 | false | 5 | 0 | Matches expected | Pass |
| W-1078 | 0.01 | 10000.01 | false | 6 | 0 | Matches expected | Pass |
| W-1079 | 0.01 | 10000.01 | false | 7 | 0 | Matches expected | Pass |
| W-1080 | 0.01 | 10000.01 | false | 8 | Unspecified | 0 | Observation |
| W-1081 | 500 | -0.01 | false | 0 | Unspecified | 1 | Observation |
| W-1082 | 500 | -0.01 | false | 1 | Unspecified | 1 | Observation |
| W-1083 | 500 | -0.01 | false | 2 | Unspecified | 1 | Observation |
| W-1084 | 500 | -0.01 | false | 3 | Unspecified | 1 | Observation |
| W-1085 | 500 | -0.01 | false | 4 | Unspecified | 1 | Observation |
| W-1086 | 500 | -0.01 | false | 5 | Unspecified | 1 | Observation |
| W-1087 | 500 | -0.01 | false | 6 | Unspecified | 1 | Observation |
| W-1088 | 500 | -0.01 | false | 7 | Unspecified | 1 | Observation |
| W-1089 | 500 | -0.01 | false | 8 | Unspecified | 1 | Observation |
| W-1090 | 500 | 0 | false | 0 | Unspecified | 1 | Observation |
| W-1091 | 500 | 0 | false | 1 | 1.5 | 1 | Fail (W-F02) |
| W-1092 | 500 | 0 | false | 2 | 1.5 | 1 | Fail (W-F02) |
| W-1093 | 500 | 0 | false | 3 | 1.5 | 1 | Fail (W-F02) |
| W-1094 | 500 | 0 | false | 4 | 1.5 | 1 | Fail (W-F02) |
| W-1095 | 500 | 0 | false | 5 | 1.5 | 1 | Fail (W-F02) |
| W-1096 | 500 | 0 | false | 6 | 1.5 | 1 | Fail (W-F02) |
| W-1097 | 500 | 0 | false | 7 | 1.5 | 1 | Fail (W-F02) |
| W-1098 | 500 | 0 | false | 8 | Unspecified | 1 | Observation |
| W-1099 | 500 | 0.01 | false | 0 | Unspecified | 1 | Observation |
| W-1100 | 500 | 0.01 | false | 1 | 1.5 | 1 | Fail (W-F02) |
| W-1101 | 500 | 0.01 | false | 2 | 1.5 | 1 | Fail (W-F02) |
| W-1102 | 500 | 0.01 | false | 3 | 1.5 | 1 | Fail (W-F02) |
| W-1103 | 500 | 0.01 | false | 4 | 1.5 | 1 | Fail (W-F02) |
| W-1104 | 500 | 0.01 | false | 5 | 1.5 | 1 | Fail (W-F02) |
| W-1105 | 500 | 0.01 | false | 6 | 1.5 | 1 | Fail (W-F02) |
| W-1106 | 500 | 0.01 | false | 7 | 1.5 | 1 | Fail (W-F02) |
| W-1107 | 500 | 0.01 | false | 8 | Unspecified | 1 | Observation |
| W-1108 | 500 | 999.99 | false | 0 | Unspecified | 1 | Observation |
| W-1109 | 500 | 999.99 | false | 1 | 1.5 | 1 | Fail (W-F02) |
| W-1110 | 500 | 999.99 | false | 2 | 1.5 | 1 | Fail (W-F02) |
| W-1111 | 500 | 999.99 | false | 3 | 1.5 | 1 | Fail (W-F02) |
| W-1112 | 500 | 999.99 | false | 4 | 1.5 | 1 | Fail (W-F02) |
| W-1113 | 500 | 999.99 | false | 5 | 1.5 | 1 | Fail (W-F02) |
| W-1114 | 500 | 999.99 | false | 6 | 1.5 | 1 | Fail (W-F02) |
| W-1115 | 500 | 999.99 | false | 7 | 1.5 | 1 | Fail (W-F02) |
| W-1116 | 500 | 999.99 | false | 8 | Unspecified | 1 | Observation |
| W-1117 | 500 | 1000 | false | 0 | Unspecified | 0.5 | Observation |
| W-1118 | 500 | 1000 | false | 1 | 0.5 | Matches expected | Pass |
| W-1119 | 500 | 1000 | false | 2 | 0.5 | Matches expected | Pass |
| W-1120 | 500 | 1000 | false | 3 | 0.5 | Matches expected | Pass |
| W-1121 | 500 | 1000 | false | 4 | 0.5 | Matches expected | Pass |
| W-1122 | 500 | 1000 | false | 5 | 0.5 | Matches expected | Pass |
| W-1123 | 500 | 1000 | false | 6 | 0.5 | Matches expected | Pass |
| W-1124 | 500 | 1000 | false | 7 | 0.5 | Matches expected | Pass |
| W-1125 | 500 | 1000 | false | 8 | Unspecified | 0.5 | Observation |
| W-1126 | 500 | 1000.01 | false | 0 | Unspecified | 0.5 | Observation |
| W-1127 | 500 | 1000.01 | false | 1 | 0.5 | Matches expected | Pass |
| W-1128 | 500 | 1000.01 | false | 2 | 0.5 | Matches expected | Pass |
| W-1129 | 500 | 1000.01 | false | 3 | 0.5 | Matches expected | Pass |
| W-1130 | 500 | 1000.01 | false | 4 | 0.5 | Matches expected | Pass |
| W-1131 | 500 | 1000.01 | false | 5 | 0.5 | Matches expected | Pass |
| W-1132 | 500 | 1000.01 | false | 6 | 0.5 | Matches expected | Pass |
| W-1133 | 500 | 1000.01 | false | 7 | 0.5 | Matches expected | Pass |
| W-1134 | 500 | 1000.01 | false | 8 | Unspecified | 0.5 | Observation |
| W-1135 | 500 | 4999.99 | false | 0 | Unspecified | 0.5 | Observation |
| W-1136 | 500 | 4999.99 | false | 1 | 0.5 | Matches expected | Pass |
| W-1137 | 500 | 4999.99 | false | 2 | 0.5 | Matches expected | Pass |
| W-1138 | 500 | 4999.99 | false | 3 | 0.5 | Matches expected | Pass |
| W-1139 | 500 | 4999.99 | false | 4 | 0.5 | Matches expected | Pass |
| W-1140 | 500 | 4999.99 | false | 5 | 0.5 | Matches expected | Pass |
| W-1141 | 500 | 4999.99 | false | 6 | 0.5 | Matches expected | Pass |
| W-1142 | 500 | 4999.99 | false | 7 | 0.5 | Matches expected | Pass |
| W-1143 | 500 | 4999.99 | false | 8 | Unspecified | 0.5 | Observation |
| W-1144 | 500 | 5000 | false | 0 | Unspecified | 0.5 | Observation |
| W-1145 | 500 | 5000 | false | 1 | 0 | 0.5 | Fail (W-F03) |
| W-1146 | 500 | 5000 | false | 2 | 0 | 0.5 | Fail (W-F03) |
| W-1147 | 500 | 5000 | false | 3 | 0 | 0.5 | Fail (W-F03) |
| W-1148 | 500 | 5000 | false | 4 | 0 | 0.5 | Fail (W-F03) |
| W-1149 | 500 | 5000 | false | 5 | 0 | 0.5 | Fail (W-F03) |
| W-1150 | 500 | 5000 | false | 6 | 0 | 0.5 | Fail (W-F03) |
| W-1151 | 500 | 5000 | false | 7 | 0 | 0.5 | Fail (W-F03) |
| W-1152 | 500 | 5000 | false | 8 | Unspecified | 0.5 | Observation |
| W-1153 | 500 | 5000.01 | false | 0 | Unspecified | 0.5 | Observation |
| W-1154 | 500 | 5000.01 | false | 1 | 0 | 0.5 | Fail (W-F03) |
| W-1155 | 500 | 5000.01 | false | 2 | 0 | 0.5 | Fail (W-F03) |
| W-1156 | 500 | 5000.01 | false | 3 | 0 | 0.5 | Fail (W-F03) |
| W-1157 | 500 | 5000.01 | false | 4 | 0 | 0.5 | Fail (W-F03) |
| W-1158 | 500 | 5000.01 | false | 5 | 0 | 0.5 | Fail (W-F03) |
| W-1159 | 500 | 5000.01 | false | 6 | 0 | 0.5 | Fail (W-F03) |
| W-1160 | 500 | 5000.01 | false | 7 | 0 | 0.5 | Fail (W-F03) |
| W-1161 | 500 | 5000.01 | false | 8 | Unspecified | 0.5 | Observation |
| W-1162 | 500 | 9999.99 | false | 0 | Unspecified | 0.5 | Observation |
| W-1163 | 500 | 9999.99 | false | 1 | 0 | 0.5 | Fail (W-F03) |
| W-1164 | 500 | 9999.99 | false | 2 | 0 | 0.5 | Fail (W-F03) |
| W-1165 | 500 | 9999.99 | false | 3 | 0 | 0.5 | Fail (W-F03) |
| W-1166 | 500 | 9999.99 | false | 4 | 0 | 0.5 | Fail (W-F03) |
| W-1167 | 500 | 9999.99 | false | 5 | 0 | 0.5 | Fail (W-F03) |
| W-1168 | 500 | 9999.99 | false | 6 | 0 | 0.5 | Fail (W-F03) |
| W-1169 | 500 | 9999.99 | false | 7 | 0 | 0.5 | Fail (W-F03) |
| W-1170 | 500 | 9999.99 | false | 8 | Unspecified | 0.5 | Observation |
| W-1171 | 500 | 10000 | false | 0 | Unspecified | 0 | Observation |
| W-1172 | 500 | 10000 | false | 1 | 0 | Matches expected | Pass |
| W-1173 | 500 | 10000 | false | 2 | 0 | Matches expected | Pass |
| W-1174 | 500 | 10000 | false | 3 | 0 | Matches expected | Pass |
| W-1175 | 500 | 10000 | false | 4 | 0 | Matches expected | Pass |
| W-1176 | 500 | 10000 | false | 5 | 0 | Matches expected | Pass |
| W-1177 | 500 | 10000 | false | 6 | 0 | Matches expected | Pass |
| W-1178 | 500 | 10000 | false | 7 | 0 | Matches expected | Pass |
| W-1179 | 500 | 10000 | false | 8 | Unspecified | 0 | Observation |
| W-1180 | 500 | 10000.01 | false | 0 | Unspecified | 0 | Observation |
| W-1181 | 500 | 10000.01 | false | 1 | 0 | Matches expected | Pass |
| W-1182 | 500 | 10000.01 | false | 2 | 0 | Matches expected | Pass |
| W-1183 | 500 | 10000.01 | false | 3 | 0 | Matches expected | Pass |
| W-1184 | 500 | 10000.01 | false | 4 | 0 | Matches expected | Pass |
| W-1185 | 500 | 10000.01 | false | 5 | 0 | Matches expected | Pass |
| W-1186 | 500 | 10000.01 | false | 6 | 0 | Matches expected | Pass |
| W-1187 | 500 | 10000.01 | false | 7 | 0 | Matches expected | Pass |
| W-1188 | 500 | 10000.01 | false | 8 | Unspecified | 0 | Observation |
| W-1189 | 999.99 | -0.01 | false | 0 | Unspecified | 1.99998 | Observation |
| W-1190 | 999.99 | -0.01 | false | 1 | Unspecified | 1.99998 | Observation |
| W-1191 | 999.99 | -0.01 | false | 2 | Unspecified | 1.99998 | Observation |
| W-1192 | 999.99 | -0.01 | false | 3 | Unspecified | 1.99998 | Observation |
| W-1193 | 999.99 | -0.01 | false | 4 | Unspecified | 1.99998 | Observation |
| W-1194 | 999.99 | -0.01 | false | 5 | Unspecified | 1.99998 | Observation |
| W-1195 | 999.99 | -0.01 | false | 6 | Unspecified | 1.99998 | Observation |
| W-1196 | 999.99 | -0.01 | false | 7 | Unspecified | 1.99998 | Observation |
| W-1197 | 999.99 | -0.01 | false | 8 | Unspecified | 1.99998 | Observation |
| W-1198 | 999.99 | 0 | false | 0 | Unspecified | 1.99998 | Observation |
| W-1199 | 999.99 | 0 | false | 1 | 2.99997 | 1.99998 | Fail (W-F02) |
| W-1200 | 999.99 | 0 | false | 2 | 2.99997 | 1.99998 | Fail (W-F02) |
| W-1201 | 999.99 | 0 | false | 3 | 2.99997 | 1.99998 | Fail (W-F02) |
| W-1202 | 999.99 | 0 | false | 4 | 2.99997 | 1.99998 | Fail (W-F02) |
| W-1203 | 999.99 | 0 | false | 5 | 2.99997 | 1.99998 | Fail (W-F02) |
| W-1204 | 999.99 | 0 | false | 6 | 2.99997 | 1.99998 | Fail (W-F02) |
| W-1205 | 999.99 | 0 | false | 7 | 2.99997 | 1.99998 | Fail (W-F02) |
| W-1206 | 999.99 | 0 | false | 8 | Unspecified | 1.99998 | Observation |
| W-1207 | 999.99 | 0.01 | false | 0 | Unspecified | 1.99998 | Observation |
| W-1208 | 999.99 | 0.01 | false | 1 | 2.99997 | 1.99998 | Fail (W-F02) |
| W-1209 | 999.99 | 0.01 | false | 2 | 2.99997 | 1.99998 | Fail (W-F02) |
| W-1210 | 999.99 | 0.01 | false | 3 | 2.99997 | 1.99998 | Fail (W-F02) |
| W-1211 | 999.99 | 0.01 | false | 4 | 2.99997 | 1.99998 | Fail (W-F02) |
| W-1212 | 999.99 | 0.01 | false | 5 | 2.99997 | 1.99998 | Fail (W-F02) |
| W-1213 | 999.99 | 0.01 | false | 6 | 2.99997 | 1.99998 | Fail (W-F02) |
| W-1214 | 999.99 | 0.01 | false | 7 | 2.99997 | 1.99998 | Fail (W-F02) |
| W-1215 | 999.99 | 0.01 | false | 8 | Unspecified | 1.99998 | Observation |
| W-1216 | 999.99 | 999.99 | false | 0 | Unspecified | 1.99998 | Observation |
| W-1217 | 999.99 | 999.99 | false | 1 | 2.99997 | 1.99998 | Fail (W-F02) |
| W-1218 | 999.99 | 999.99 | false | 2 | 2.99997 | 1.99998 | Fail (W-F02) |
| W-1219 | 999.99 | 999.99 | false | 3 | 2.99997 | 1.99998 | Fail (W-F02) |
| W-1220 | 999.99 | 999.99 | false | 4 | 2.99997 | 1.99998 | Fail (W-F02) |
| W-1221 | 999.99 | 999.99 | false | 5 | 2.99997 | 1.99998 | Fail (W-F02) |
| W-1222 | 999.99 | 999.99 | false | 6 | 2.99997 | 1.99998 | Fail (W-F02) |
| W-1223 | 999.99 | 999.99 | false | 7 | 2.99997 | 1.99998 | Fail (W-F02) |
| W-1224 | 999.99 | 999.99 | false | 8 | Unspecified | 1.99998 | Observation |
| W-1225 | 999.99 | 1000 | false | 0 | Unspecified | 0.99999 | Observation |
| W-1226 | 999.99 | 1000 | false | 1 | 0.99999 | Matches expected | Pass |
| W-1227 | 999.99 | 1000 | false | 2 | 0.99999 | Matches expected | Pass |
| W-1228 | 999.99 | 1000 | false | 3 | 0.99999 | Matches expected | Pass |
| W-1229 | 999.99 | 1000 | false | 4 | 0.99999 | Matches expected | Pass |
| W-1230 | 999.99 | 1000 | false | 5 | 0.99999 | Matches expected | Pass |
| W-1231 | 999.99 | 1000 | false | 6 | 0.99999 | Matches expected | Pass |
| W-1232 | 999.99 | 1000 | false | 7 | 0.99999 | Matches expected | Pass |
| W-1233 | 999.99 | 1000 | false | 8 | Unspecified | 0.99999 | Observation |
| W-1234 | 999.99 | 1000.01 | false | 0 | Unspecified | 0.99999 | Observation |
| W-1235 | 999.99 | 1000.01 | false | 1 | 0.99999 | Matches expected | Pass |
| W-1236 | 999.99 | 1000.01 | false | 2 | 0.99999 | Matches expected | Pass |
| W-1237 | 999.99 | 1000.01 | false | 3 | 0.99999 | Matches expected | Pass |
| W-1238 | 999.99 | 1000.01 | false | 4 | 0.99999 | Matches expected | Pass |
| W-1239 | 999.99 | 1000.01 | false | 5 | 0.99999 | Matches expected | Pass |
| W-1240 | 999.99 | 1000.01 | false | 6 | 0.99999 | Matches expected | Pass |
| W-1241 | 999.99 | 1000.01 | false | 7 | 0.99999 | Matches expected | Pass |
| W-1242 | 999.99 | 1000.01 | false | 8 | Unspecified | 0.99999 | Observation |
| W-1243 | 999.99 | 4999.99 | false | 0 | Unspecified | 0.99999 | Observation |
| W-1244 | 999.99 | 4999.99 | false | 1 | 0.99999 | Matches expected | Pass |
| W-1245 | 999.99 | 4999.99 | false | 2 | 0.99999 | Matches expected | Pass |
| W-1246 | 999.99 | 4999.99 | false | 3 | 0.99999 | Matches expected | Pass |
| W-1247 | 999.99 | 4999.99 | false | 4 | 0.99999 | Matches expected | Pass |
| W-1248 | 999.99 | 4999.99 | false | 5 | 0.99999 | Matches expected | Pass |
| W-1249 | 999.99 | 4999.99 | false | 6 | 0.99999 | Matches expected | Pass |
| W-1250 | 999.99 | 4999.99 | false | 7 | 0.99999 | Matches expected | Pass |
| W-1251 | 999.99 | 4999.99 | false | 8 | Unspecified | 0.99999 | Observation |
| W-1252 | 999.99 | 5000 | false | 0 | Unspecified | 0.99999 | Observation |
| W-1253 | 999.99 | 5000 | false | 1 | 0 | 0.99999 | Fail (W-F03) |
| W-1254 | 999.99 | 5000 | false | 2 | 0 | 0.99999 | Fail (W-F03) |
| W-1255 | 999.99 | 5000 | false | 3 | 0 | 0.99999 | Fail (W-F03) |
| W-1256 | 999.99 | 5000 | false | 4 | 0 | 0.99999 | Fail (W-F03) |
| W-1257 | 999.99 | 5000 | false | 5 | 0 | 0.99999 | Fail (W-F03) |
| W-1258 | 999.99 | 5000 | false | 6 | 0 | 0.99999 | Fail (W-F03) |
| W-1259 | 999.99 | 5000 | false | 7 | 0 | 0.99999 | Fail (W-F03) |
| W-1260 | 999.99 | 5000 | false | 8 | Unspecified | 0.99999 | Observation |
| W-1261 | 999.99 | 5000.01 | false | 0 | Unspecified | 0.99999 | Observation |
| W-1262 | 999.99 | 5000.01 | false | 1 | 0 | 0.99999 | Fail (W-F03) |
| W-1263 | 999.99 | 5000.01 | false | 2 | 0 | 0.99999 | Fail (W-F03) |
| W-1264 | 999.99 | 5000.01 | false | 3 | 0 | 0.99999 | Fail (W-F03) |
| W-1265 | 999.99 | 5000.01 | false | 4 | 0 | 0.99999 | Fail (W-F03) |
| W-1266 | 999.99 | 5000.01 | false | 5 | 0 | 0.99999 | Fail (W-F03) |
| W-1267 | 999.99 | 5000.01 | false | 6 | 0 | 0.99999 | Fail (W-F03) |
| W-1268 | 999.99 | 5000.01 | false | 7 | 0 | 0.99999 | Fail (W-F03) |
| W-1269 | 999.99 | 5000.01 | false | 8 | Unspecified | 0.99999 | Observation |
| W-1270 | 999.99 | 9999.99 | false | 0 | Unspecified | 0.99999 | Observation |
| W-1271 | 999.99 | 9999.99 | false | 1 | 0 | 0.99999 | Fail (W-F03) |
| W-1272 | 999.99 | 9999.99 | false | 2 | 0 | 0.99999 | Fail (W-F03) |
| W-1273 | 999.99 | 9999.99 | false | 3 | 0 | 0.99999 | Fail (W-F03) |
| W-1274 | 999.99 | 9999.99 | false | 4 | 0 | 0.99999 | Fail (W-F03) |
| W-1275 | 999.99 | 9999.99 | false | 5 | 0 | 0.99999 | Fail (W-F03) |
| W-1276 | 999.99 | 9999.99 | false | 6 | 0 | 0.99999 | Fail (W-F03) |
| W-1277 | 999.99 | 9999.99 | false | 7 | 0 | 0.99999 | Fail (W-F03) |
| W-1278 | 999.99 | 9999.99 | false | 8 | Unspecified | 0.99999 | Observation |
| W-1279 | 999.99 | 10000 | false | 0 | Unspecified | 0 | Observation |
| W-1280 | 999.99 | 10000 | false | 1 | 0 | Matches expected | Pass |
| W-1281 | 999.99 | 10000 | false | 2 | 0 | Matches expected | Pass |
| W-1282 | 999.99 | 10000 | false | 3 | 0 | Matches expected | Pass |
| W-1283 | 999.99 | 10000 | false | 4 | 0 | Matches expected | Pass |
| W-1284 | 999.99 | 10000 | false | 5 | 0 | Matches expected | Pass |
| W-1285 | 999.99 | 10000 | false | 6 | 0 | Matches expected | Pass |
| W-1286 | 999.99 | 10000 | false | 7 | 0 | Matches expected | Pass |
| W-1287 | 999.99 | 10000 | false | 8 | Unspecified | 0 | Observation |
| W-1288 | 999.99 | 10000.01 | false | 0 | Unspecified | 0 | Observation |
| W-1289 | 999.99 | 10000.01 | false | 1 | 0 | Matches expected | Pass |
| W-1290 | 999.99 | 10000.01 | false | 2 | 0 | Matches expected | Pass |
| W-1291 | 999.99 | 10000.01 | false | 3 | 0 | Matches expected | Pass |
| W-1292 | 999.99 | 10000.01 | false | 4 | 0 | Matches expected | Pass |
| W-1293 | 999.99 | 10000.01 | false | 5 | 0 | Matches expected | Pass |
| W-1294 | 999.99 | 10000.01 | false | 6 | 0 | Matches expected | Pass |
| W-1295 | 999.99 | 10000.01 | false | 7 | 0 | Matches expected | Pass |
| W-1296 | 999.99 | 10000.01 | false | 8 | Unspecified | 0 | Observation |
| W-1297 | 1000 | -0.01 | false | 0 | Unspecified | 2 | Observation |
| W-1298 | 1000 | -0.01 | false | 1 | Unspecified | 2 | Observation |
| W-1299 | 1000 | -0.01 | false | 2 | Unspecified | 2 | Observation |
| W-1300 | 1000 | -0.01 | false | 3 | Unspecified | 2 | Observation |
| W-1301 | 1000 | -0.01 | false | 4 | Unspecified | 2 | Observation |
| W-1302 | 1000 | -0.01 | false | 5 | Unspecified | 2 | Observation |
| W-1303 | 1000 | -0.01 | false | 6 | Unspecified | 2 | Observation |
| W-1304 | 1000 | -0.01 | false | 7 | Unspecified | 2 | Observation |
| W-1305 | 1000 | -0.01 | false | 8 | Unspecified | 2 | Observation |
| W-1306 | 1000 | 0 | false | 0 | Unspecified | 2 | Observation |
| W-1307 | 1000 | 0 | false | 1 | 3 | 2 | Fail (W-F02) |
| W-1308 | 1000 | 0 | false | 2 | 3 | 2 | Fail (W-F02) |
| W-1309 | 1000 | 0 | false | 3 | 3 | 2 | Fail (W-F02) |
| W-1310 | 1000 | 0 | false | 4 | 3 | 2 | Fail (W-F02) |
| W-1311 | 1000 | 0 | false | 5 | 3 | 2 | Fail (W-F02) |
| W-1312 | 1000 | 0 | false | 6 | 3 | 2 | Fail (W-F02) |
| W-1313 | 1000 | 0 | false | 7 | 3 | 2 | Fail (W-F02) |
| W-1314 | 1000 | 0 | false | 8 | Unspecified | 2 | Observation |
| W-1315 | 1000 | 0.01 | false | 0 | Unspecified | 2 | Observation |
| W-1316 | 1000 | 0.01 | false | 1 | 3 | 2 | Fail (W-F02) |
| W-1317 | 1000 | 0.01 | false | 2 | 3 | 2 | Fail (W-F02) |
| W-1318 | 1000 | 0.01 | false | 3 | 3 | 2 | Fail (W-F02) |
| W-1319 | 1000 | 0.01 | false | 4 | 3 | 2 | Fail (W-F02) |
| W-1320 | 1000 | 0.01 | false | 5 | 3 | 2 | Fail (W-F02) |
| W-1321 | 1000 | 0.01 | false | 6 | 3 | 2 | Fail (W-F02) |
| W-1322 | 1000 | 0.01 | false | 7 | 3 | 2 | Fail (W-F02) |
| W-1323 | 1000 | 0.01 | false | 8 | Unspecified | 2 | Observation |
| W-1324 | 1000 | 999.99 | false | 0 | Unspecified | 2 | Observation |
| W-1325 | 1000 | 999.99 | false | 1 | 3 | 2 | Fail (W-F02) |
| W-1326 | 1000 | 999.99 | false | 2 | 3 | 2 | Fail (W-F02) |
| W-1327 | 1000 | 999.99 | false | 3 | 3 | 2 | Fail (W-F02) |
| W-1328 | 1000 | 999.99 | false | 4 | 3 | 2 | Fail (W-F02) |
| W-1329 | 1000 | 999.99 | false | 5 | 3 | 2 | Fail (W-F02) |
| W-1330 | 1000 | 999.99 | false | 6 | 3 | 2 | Fail (W-F02) |
| W-1331 | 1000 | 999.99 | false | 7 | 3 | 2 | Fail (W-F02) |
| W-1332 | 1000 | 999.99 | false | 8 | Unspecified | 2 | Observation |
| W-1333 | 1000 | 1000 | false | 0 | Unspecified | 1 | Observation |
| W-1334 | 1000 | 1000 | false | 1 | 1 | Matches expected | Pass |
| W-1335 | 1000 | 1000 | false | 2 | 1 | Matches expected | Pass |
| W-1336 | 1000 | 1000 | false | 3 | 1 | Matches expected | Pass |
| W-1337 | 1000 | 1000 | false | 4 | 1 | Matches expected | Pass |
| W-1338 | 1000 | 1000 | false | 5 | 1 | Matches expected | Pass |
| W-1339 | 1000 | 1000 | false | 6 | 1 | Matches expected | Pass |
| W-1340 | 1000 | 1000 | false | 7 | 1 | Matches expected | Pass |
| W-1341 | 1000 | 1000 | false | 8 | Unspecified | 1 | Observation |
| W-1342 | 1000 | 1000.01 | false | 0 | Unspecified | 1 | Observation |
| W-1343 | 1000 | 1000.01 | false | 1 | 1 | Matches expected | Pass |
| W-1344 | 1000 | 1000.01 | false | 2 | 1 | Matches expected | Pass |
| W-1345 | 1000 | 1000.01 | false | 3 | 1 | Matches expected | Pass |
| W-1346 | 1000 | 1000.01 | false | 4 | 1 | Matches expected | Pass |
| W-1347 | 1000 | 1000.01 | false | 5 | 1 | Matches expected | Pass |
| W-1348 | 1000 | 1000.01 | false | 6 | 1 | Matches expected | Pass |
| W-1349 | 1000 | 1000.01 | false | 7 | 1 | Matches expected | Pass |
| W-1350 | 1000 | 1000.01 | false | 8 | Unspecified | 1 | Observation |
| W-1351 | 1000 | 4999.99 | false | 0 | Unspecified | 1 | Observation |
| W-1352 | 1000 | 4999.99 | false | 1 | 1 | Matches expected | Pass |
| W-1353 | 1000 | 4999.99 | false | 2 | 1 | Matches expected | Pass |
| W-1354 | 1000 | 4999.99 | false | 3 | 1 | Matches expected | Pass |
| W-1355 | 1000 | 4999.99 | false | 4 | 1 | Matches expected | Pass |
| W-1356 | 1000 | 4999.99 | false | 5 | 1 | Matches expected | Pass |
| W-1357 | 1000 | 4999.99 | false | 6 | 1 | Matches expected | Pass |
| W-1358 | 1000 | 4999.99 | false | 7 | 1 | Matches expected | Pass |
| W-1359 | 1000 | 4999.99 | false | 8 | Unspecified | 1 | Observation |
| W-1360 | 1000 | 5000 | false | 0 | Unspecified | 1 | Observation |
| W-1361 | 1000 | 5000 | false | 1 | 0 | 1 | Fail (W-F03) |
| W-1362 | 1000 | 5000 | false | 2 | 0 | 1 | Fail (W-F03) |
| W-1363 | 1000 | 5000 | false | 3 | 0 | 1 | Fail (W-F03) |
| W-1364 | 1000 | 5000 | false | 4 | 0 | 1 | Fail (W-F03) |
| W-1365 | 1000 | 5000 | false | 5 | 0 | 1 | Fail (W-F03) |
| W-1366 | 1000 | 5000 | false | 6 | 0 | 1 | Fail (W-F03) |
| W-1367 | 1000 | 5000 | false | 7 | 0 | 1 | Fail (W-F03) |
| W-1368 | 1000 | 5000 | false | 8 | Unspecified | 1 | Observation |
| W-1369 | 1000 | 5000.01 | false | 0 | Unspecified | 1 | Observation |
| W-1370 | 1000 | 5000.01 | false | 1 | 0 | 1 | Fail (W-F03) |
| W-1371 | 1000 | 5000.01 | false | 2 | 0 | 1 | Fail (W-F03) |
| W-1372 | 1000 | 5000.01 | false | 3 | 0 | 1 | Fail (W-F03) |
| W-1373 | 1000 | 5000.01 | false | 4 | 0 | 1 | Fail (W-F03) |
| W-1374 | 1000 | 5000.01 | false | 5 | 0 | 1 | Fail (W-F03) |
| W-1375 | 1000 | 5000.01 | false | 6 | 0 | 1 | Fail (W-F03) |
| W-1376 | 1000 | 5000.01 | false | 7 | 0 | 1 | Fail (W-F03) |
| W-1377 | 1000 | 5000.01 | false | 8 | Unspecified | 1 | Observation |
| W-1378 | 1000 | 9999.99 | false | 0 | Unspecified | 1 | Observation |
| W-1379 | 1000 | 9999.99 | false | 1 | 0 | 1 | Fail (W-F03) |
| W-1380 | 1000 | 9999.99 | false | 2 | 0 | 1 | Fail (W-F03) |
| W-1381 | 1000 | 9999.99 | false | 3 | 0 | 1 | Fail (W-F03) |
| W-1382 | 1000 | 9999.99 | false | 4 | 0 | 1 | Fail (W-F03) |
| W-1383 | 1000 | 9999.99 | false | 5 | 0 | 1 | Fail (W-F03) |
| W-1384 | 1000 | 9999.99 | false | 6 | 0 | 1 | Fail (W-F03) |
| W-1385 | 1000 | 9999.99 | false | 7 | 0 | 1 | Fail (W-F03) |
| W-1386 | 1000 | 9999.99 | false | 8 | Unspecified | 1 | Observation |
| W-1387 | 1000 | 10000 | false | 0 | Unspecified | 0 | Observation |
| W-1388 | 1000 | 10000 | false | 1 | 0 | Matches expected | Pass |
| W-1389 | 1000 | 10000 | false | 2 | 0 | Matches expected | Pass |
| W-1390 | 1000 | 10000 | false | 3 | 0 | Matches expected | Pass |
| W-1391 | 1000 | 10000 | false | 4 | 0 | Matches expected | Pass |
| W-1392 | 1000 | 10000 | false | 5 | 0 | Matches expected | Pass |
| W-1393 | 1000 | 10000 | false | 6 | 0 | Matches expected | Pass |
| W-1394 | 1000 | 10000 | false | 7 | 0 | Matches expected | Pass |
| W-1395 | 1000 | 10000 | false | 8 | Unspecified | 0 | Observation |
| W-1396 | 1000 | 10000.01 | false | 0 | Unspecified | 0 | Observation |
| W-1397 | 1000 | 10000.01 | false | 1 | 0 | Matches expected | Pass |
| W-1398 | 1000 | 10000.01 | false | 2 | 0 | Matches expected | Pass |
| W-1399 | 1000 | 10000.01 | false | 3 | 0 | Matches expected | Pass |
| W-1400 | 1000 | 10000.01 | false | 4 | 0 | Matches expected | Pass |
| W-1401 | 1000 | 10000.01 | false | 5 | 0 | Matches expected | Pass |
| W-1402 | 1000 | 10000.01 | false | 6 | 0 | Matches expected | Pass |
| W-1403 | 1000 | 10000.01 | false | 7 | 0 | Matches expected | Pass |
| W-1404 | 1000 | 10000.01 | false | 8 | Unspecified | 0 | Observation |
| W-1405 | 1000.01 | -0.01 | false | 0 | Unspecified | 2.00002 | Observation |
| W-1406 | 1000.01 | -0.01 | false | 1 | Unspecified | 2.00002 | Observation |
| W-1407 | 1000.01 | -0.01 | false | 2 | Unspecified | 2.00002 | Observation |
| W-1408 | 1000.01 | -0.01 | false | 3 | Unspecified | 2.00002 | Observation |
| W-1409 | 1000.01 | -0.01 | false | 4 | Unspecified | 2.00002 | Observation |
| W-1410 | 1000.01 | -0.01 | false | 5 | Unspecified | 2.00002 | Observation |
| W-1411 | 1000.01 | -0.01 | false | 6 | Unspecified | 2.00002 | Observation |
| W-1412 | 1000.01 | -0.01 | false | 7 | Unspecified | 2.00002 | Observation |
| W-1413 | 1000.01 | -0.01 | false | 8 | Unspecified | 2.00002 | Observation |
| W-1414 | 1000.01 | 0 | false | 0 | Unspecified | 2.00002 | Observation |
| W-1415 | 1000.01 | 0 | false | 1 | 3.00003 | 2.00002 | Fail (W-F02) |
| W-1416 | 1000.01 | 0 | false | 2 | 3.00003 | 2.00002 | Fail (W-F02) |
| W-1417 | 1000.01 | 0 | false | 3 | 3.00003 | 2.00002 | Fail (W-F02) |
| W-1418 | 1000.01 | 0 | false | 4 | 3.00003 | 2.00002 | Fail (W-F02) |
| W-1419 | 1000.01 | 0 | false | 5 | 3.00003 | 2.00002 | Fail (W-F02) |
| W-1420 | 1000.01 | 0 | false | 6 | 3.00003 | 2.00002 | Fail (W-F02) |
| W-1421 | 1000.01 | 0 | false | 7 | 3.00003 | 2.00002 | Fail (W-F02) |
| W-1422 | 1000.01 | 0 | false | 8 | Unspecified | 2.00002 | Observation |
| W-1423 | 1000.01 | 0.01 | false | 0 | Unspecified | 2.00002 | Observation |
| W-1424 | 1000.01 | 0.01 | false | 1 | 3.00003 | 2.00002 | Fail (W-F02) |
| W-1425 | 1000.01 | 0.01 | false | 2 | 3.00003 | 2.00002 | Fail (W-F02) |
| W-1426 | 1000.01 | 0.01 | false | 3 | 3.00003 | 2.00002 | Fail (W-F02) |
| W-1427 | 1000.01 | 0.01 | false | 4 | 3.00003 | 2.00002 | Fail (W-F02) |
| W-1428 | 1000.01 | 0.01 | false | 5 | 3.00003 | 2.00002 | Fail (W-F02) |
| W-1429 | 1000.01 | 0.01 | false | 6 | 3.00003 | 2.00002 | Fail (W-F02) |
| W-1430 | 1000.01 | 0.01 | false | 7 | 3.00003 | 2.00002 | Fail (W-F02) |
| W-1431 | 1000.01 | 0.01 | false | 8 | Unspecified | 2.00002 | Observation |
| W-1432 | 1000.01 | 999.99 | false | 0 | Unspecified | 2.00002 | Observation |
| W-1433 | 1000.01 | 999.99 | false | 1 | 3.00003 | 2.00002 | Fail (W-F02) |
| W-1434 | 1000.01 | 999.99 | false | 2 | 3.00003 | 2.00002 | Fail (W-F02) |
| W-1435 | 1000.01 | 999.99 | false | 3 | 3.00003 | 2.00002 | Fail (W-F02) |
| W-1436 | 1000.01 | 999.99 | false | 4 | 3.00003 | 2.00002 | Fail (W-F02) |
| W-1437 | 1000.01 | 999.99 | false | 5 | 3.00003 | 2.00002 | Fail (W-F02) |
| W-1438 | 1000.01 | 999.99 | false | 6 | 3.00003 | 2.00002 | Fail (W-F02) |
| W-1439 | 1000.01 | 999.99 | false | 7 | 3.00003 | 2.00002 | Fail (W-F02) |
| W-1440 | 1000.01 | 999.99 | false | 8 | Unspecified | 2.00002 | Observation |
| W-1441 | 1000.01 | 1000 | false | 0 | Unspecified | 1.00001 | Observation |
| W-1442 | 1000.01 | 1000 | false | 1 | 1.00001 | Matches expected | Pass |
| W-1443 | 1000.01 | 1000 | false | 2 | 1.00001 | Matches expected | Pass |
| W-1444 | 1000.01 | 1000 | false | 3 | 1.00001 | Matches expected | Pass |
| W-1445 | 1000.01 | 1000 | false | 4 | 1.00001 | Matches expected | Pass |
| W-1446 | 1000.01 | 1000 | false | 5 | 1.00001 | Matches expected | Pass |
| W-1447 | 1000.01 | 1000 | false | 6 | 1.00001 | Matches expected | Pass |
| W-1448 | 1000.01 | 1000 | false | 7 | 1.00001 | Matches expected | Pass |
| W-1449 | 1000.01 | 1000 | false | 8 | Unspecified | 1.00001 | Observation |
| W-1450 | 1000.01 | 1000.01 | false | 0 | Unspecified | 1.00001 | Observation |
| W-1451 | 1000.01 | 1000.01 | false | 1 | 1.00001 | Matches expected | Pass |
| W-1452 | 1000.01 | 1000.01 | false | 2 | 1.00001 | Matches expected | Pass |
| W-1453 | 1000.01 | 1000.01 | false | 3 | 1.00001 | Matches expected | Pass |
| W-1454 | 1000.01 | 1000.01 | false | 4 | 1.00001 | Matches expected | Pass |
| W-1455 | 1000.01 | 1000.01 | false | 5 | 1.00001 | Matches expected | Pass |
| W-1456 | 1000.01 | 1000.01 | false | 6 | 1.00001 | Matches expected | Pass |
| W-1457 | 1000.01 | 1000.01 | false | 7 | 1.00001 | Matches expected | Pass |
| W-1458 | 1000.01 | 1000.01 | false | 8 | Unspecified | 1.00001 | Observation |
| W-1459 | 1000.01 | 4999.99 | false | 0 | Unspecified | 1.00001 | Observation |
| W-1460 | 1000.01 | 4999.99 | false | 1 | 1.00001 | Matches expected | Pass |
| W-1461 | 1000.01 | 4999.99 | false | 2 | 1.00001 | Matches expected | Pass |
| W-1462 | 1000.01 | 4999.99 | false | 3 | 1.00001 | Matches expected | Pass |
| W-1463 | 1000.01 | 4999.99 | false | 4 | 1.00001 | Matches expected | Pass |
| W-1464 | 1000.01 | 4999.99 | false | 5 | 1.00001 | Matches expected | Pass |
| W-1465 | 1000.01 | 4999.99 | false | 6 | 1.00001 | Matches expected | Pass |
| W-1466 | 1000.01 | 4999.99 | false | 7 | 1.00001 | Matches expected | Pass |
| W-1467 | 1000.01 | 4999.99 | false | 8 | Unspecified | 1.00001 | Observation |
| W-1468 | 1000.01 | 5000 | false | 0 | Unspecified | 1.00001 | Observation |
| W-1469 | 1000.01 | 5000 | false | 1 | 0 | 1.00001 | Fail (W-F03) |
| W-1470 | 1000.01 | 5000 | false | 2 | 0 | 1.00001 | Fail (W-F03) |
| W-1471 | 1000.01 | 5000 | false | 3 | 0 | 1.00001 | Fail (W-F03) |
| W-1472 | 1000.01 | 5000 | false | 4 | 0 | 1.00001 | Fail (W-F03) |
| W-1473 | 1000.01 | 5000 | false | 5 | 0 | 1.00001 | Fail (W-F03) |
| W-1474 | 1000.01 | 5000 | false | 6 | 0 | 1.00001 | Fail (W-F03) |
| W-1475 | 1000.01 | 5000 | false | 7 | 0 | 1.00001 | Fail (W-F03) |
| W-1476 | 1000.01 | 5000 | false | 8 | Unspecified | 1.00001 | Observation |
| W-1477 | 1000.01 | 5000.01 | false | 0 | Unspecified | 1.00001 | Observation |
| W-1478 | 1000.01 | 5000.01 | false | 1 | 0 | 1.00001 | Fail (W-F03) |
| W-1479 | 1000.01 | 5000.01 | false | 2 | 0 | 1.00001 | Fail (W-F03) |
| W-1480 | 1000.01 | 5000.01 | false | 3 | 0 | 1.00001 | Fail (W-F03) |
| W-1481 | 1000.01 | 5000.01 | false | 4 | 0 | 1.00001 | Fail (W-F03) |
| W-1482 | 1000.01 | 5000.01 | false | 5 | 0 | 1.00001 | Fail (W-F03) |
| W-1483 | 1000.01 | 5000.01 | false | 6 | 0 | 1.00001 | Fail (W-F03) |
| W-1484 | 1000.01 | 5000.01 | false | 7 | 0 | 1.00001 | Fail (W-F03) |
| W-1485 | 1000.01 | 5000.01 | false | 8 | Unspecified | 1.00001 | Observation |
| W-1486 | 1000.01 | 9999.99 | false | 0 | Unspecified | 1.00001 | Observation |
| W-1487 | 1000.01 | 9999.99 | false | 1 | 0 | 1.00001 | Fail (W-F03) |
| W-1488 | 1000.01 | 9999.99 | false | 2 | 0 | 1.00001 | Fail (W-F03) |
| W-1489 | 1000.01 | 9999.99 | false | 3 | 0 | 1.00001 | Fail (W-F03) |
| W-1490 | 1000.01 | 9999.99 | false | 4 | 0 | 1.00001 | Fail (W-F03) |
| W-1491 | 1000.01 | 9999.99 | false | 5 | 0 | 1.00001 | Fail (W-F03) |
| W-1492 | 1000.01 | 9999.99 | false | 6 | 0 | 1.00001 | Fail (W-F03) |
| W-1493 | 1000.01 | 9999.99 | false | 7 | 0 | 1.00001 | Fail (W-F03) |
| W-1494 | 1000.01 | 9999.99 | false | 8 | Unspecified | 1.00001 | Observation |
| W-1495 | 1000.01 | 10000 | false | 0 | Unspecified | 0 | Observation |
| W-1496 | 1000.01 | 10000 | false | 1 | 0 | Matches expected | Pass |
| W-1497 | 1000.01 | 10000 | false | 2 | 0 | Matches expected | Pass |
| W-1498 | 1000.01 | 10000 | false | 3 | 0 | Matches expected | Pass |
| W-1499 | 1000.01 | 10000 | false | 4 | 0 | Matches expected | Pass |
| W-1500 | 1000.01 | 10000 | false | 5 | 0 | Matches expected | Pass |
| W-1501 | 1000.01 | 10000 | false | 6 | 0 | Matches expected | Pass |
| W-1502 | 1000.01 | 10000 | false | 7 | 0 | Matches expected | Pass |
| W-1503 | 1000.01 | 10000 | false | 8 | Unspecified | 0 | Observation |
| W-1504 | 1000.01 | 10000.01 | false | 0 | Unspecified | 0 | Observation |
| W-1505 | 1000.01 | 10000.01 | false | 1 | 0 | Matches expected | Pass |
| W-1506 | 1000.01 | 10000.01 | false | 2 | 0 | Matches expected | Pass |
| W-1507 | 1000.01 | 10000.01 | false | 3 | 0 | Matches expected | Pass |
| W-1508 | 1000.01 | 10000.01 | false | 4 | 0 | Matches expected | Pass |
| W-1509 | 1000.01 | 10000.01 | false | 5 | 0 | Matches expected | Pass |
| W-1510 | 1000.01 | 10000.01 | false | 6 | 0 | Matches expected | Pass |
| W-1511 | 1000.01 | 10000.01 | false | 7 | 0 | Matches expected | Pass |
| W-1512 | 1000.01 | 10000.01 | false | 8 | Unspecified | 0 | Observation |
