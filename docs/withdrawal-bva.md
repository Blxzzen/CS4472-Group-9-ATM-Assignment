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
Including Session and the three starter tests, it ran 1570 invocations:
365 passed, 617 failed assertions, 588 skipped and zero errors. Session still
had its existing 22 assertion failures. Production code was unchanged.

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
