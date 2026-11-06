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
  percentages without rounding to cents; JUnit comparisons will use an
  absolute tolerance of `1e-9`.

The specification gives no finite upper bound for the amount or balance.
For a finite grid, use sampling ranges of $0–$1000 for amount and
$0–$10000 for balance, with nominal values of $500 and $1000 respectively.
These upper endpoints are test-design choices, not account limits.
$1000.01 amounts and $10000.01 balances therefore remain valid fee inputs.
Zero is included as a fee-calculation input because no positive-minimum
rule is given for this method.

## Boundary values and combinations

Use one-cent neighbors for monetary boundaries. Include the balance tier
boundaries inside the sampling range as well as its endpoints. Take the
Cartesian product of all four rows rather than changing only one input at
a time; the single-fault restriction in the assignment applies to Session.

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
out-of-range day. The specification does not define their rejection behavior
or name an exception. Record these robustness observations separately,
without treating a missing `IllegalArgumentException` as a confirmed fault.

## Reference cases

These six cases check the expected calculations before expanding the full
grid. Their IDs match the supplied withdrawal case list.

| Case ID | Amount ($) | Balance ($) | Student | Day | Expected fee ($) | Rule |
|---|---:|---:|---|---|---:|---|
| W-1110 | 500 | 999.99 | false | Monday (2) | 1.50 | Below $1000: 500 × 0.003 |
| W-1119 | 500 | 1000 | false | Monday (2) | 0.50 | At $1000: 500 × 0.001 |
| W-1137 | 500 | 4999.99 | false | Monday (2) | 0.50 | Below $5000: 500 × 0.001 |
| W-1146 | 500 | 5000 | false | Monday (2) | 0.00 | At $5000: no fee |
| W-0363 | 500 | 1000 | true | Monday (2) | 0.00 | Student weekday: no fee |
| W-0368 | 500 | 1000 | true | Saturday (7) | 0.50 | Student weekend: 500 × 0.001 |

These are expected outcomes only. Withdrawal-fee execution results and fault
localization will be added after running the tests.
