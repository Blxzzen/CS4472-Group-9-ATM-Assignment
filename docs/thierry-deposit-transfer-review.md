# Deposit and Transfer Testing Review

## Deposit: Weak Robust Equivalence Class Analysis

### Test D-S01 — Incorrect Student Deposit Threshold

**Class:** DS1  
**Method:** `bank.utils.FeesCalculator.calculateDepositInterest`

**Inputs:**

- Student: true
- Amount: $100
- Account balance: $1,000

**Expected:** $0.50 (0.5% of $100)  
**Actual:** $0.20 (0.2% of $100)  
**Result:** FAIL

**Fault localization:**
The implementation checks whether the student deposit amount exceeds $100 instead of the specified $50 threshold. Consequently, a $100 deposit is incorrectly classified under the lower deposit branch, returning $0.20 instead of the expected $0.50.

**Additional observation:**
The student high-deposit balance threshold and fee multipliers also differ from the specification. These require further examination across the other student equivalence classes.

### Test D-S02 — Additional Evidence of Incorrect Student Threshold

**Inputs:** Student = true, amount = $100, balance = $400.

**Expected:** $0.25  
**Actual:** $0.20  
**Result:** FAIL

D-S02 further demonstrates the incorrect student deposit threshold in `calculateDepositInterest`. The implementation checks whether the deposit exceeds $100 instead of the specified $50, causing this test to enter the wrong branch.

**Fault group:** D-STUDENT-HIGH (same group as D-S01).

### Tests D-S04 and D-S05 — Incorrect Fee for Small Student Deposits

**Method:** `FeesCalculator.calculateDepositInterest`

**Equivalence class:** DS4

**Test cases:**

- D-S04: Student, $25 deposit, $400 balance.
- D-S05: Student, $50 deposit, $5,000 balance.

**Expected:** $0.00 for both tests.

**Calculated by implementation:**

- D-S04: $0.05
- D-S05: $0.10

**Fault localization:**
For student deposits of $50 or less with an account balance of $5,000 or less, the specification requires no fee. However, the implementation uses an interest multiplier of 0.002, charging 0.2% of the deposited amount.

**Suspected faulty statement:** `interestPercentage = 0.002;`

**Fault group:** D-STUDENT-LOW

Both test cases expose the same incorrect fee calculation and can be grouped under one fault.

### Tests D-N01, D-N02, D-N07, D-N08 — Incorrect Non-Student High-Deposit Logic

**Method:** `FeesCalculator.calculateDepositInterest`

**Equivalence classes:** DN1 and DN2

**Test cases:**

- D-N01: $400 deposit, $3,000 balance. Expected: $3.20.
- D-N02: $400 deposit, $2,000 balance. Expected: $1.60.
- D-N07: $250.01 deposit, $2,500 balance. Expected: $1.00004.
- D-N08: $250.01 deposit, $2,500.01 balance. Expected: $2.00008.

**Result:** All four tests FAIL.

**Fault localization:**
The implementation uses a non-student deposit threshold of $500 rather than the specified $250. Consequently, these deposits incorrectly enter the lower-deposit branch and return a fee of $0.00.

Further inspection reveals that the high-deposit balance threshold is also incorrect ($5,000 instead of $2,500), and the fee multipliers are 0.01 and 0.005 instead of the required 0.008 and 0.004.

**Suspected faulty conditions:**

- `if (amount > 500.00)`
- `if (accountBalance > 5000.00)`
- `interestPercentage = 0.01`
- `interestPercentage = 0.005`

**Fault group:** D-NONSTUDENT-HIGH

The four existing failures demonstrate incorrect branch selection. Additional tests with deposits exceeding $500 would help isolate the balance-threshold and fee-rate errors.

### Tests D-N03, D-N04, D-N05, D-N06, D-N10 — Incorrect Non-Student Low-Deposit Fees

**Method:** `FeesCalculator.calculateDepositInterest`

**Equivalence classes:** DN3 and DN4

**Test cases:**

- D-N03: $100 deposit, $11,000 balance. Expected $0.00; actual $0.50.
- D-N04: $100 deposit, $9,000 balance. Expected $0.10; actual $0.00.
- D-N05: $250 deposit, $10,000 balance. Expected $0.25; actual $0.00.
- D-N06: $250 deposit, $10,000.01 balance. Expected $0.00; actual $1.25.
- D-N10: $100 deposit, $400 balance. Expected $0.10; actual $0.00.

**Result:** All five tests FAIL.

**Fault localization:**
The implementation correctly uses $10,000 as the balance threshold but assigns incorrect fee multipliers to both branches. For balances greater than $10,000, it charges 0.5% instead of no fee. For balances of $10,000 or less, it charges no fee instead of the required 0.1%.

**Suspected faulty statements:**

- `interestPercentage = 0.005;` should be `0.0` for this branch.
- `interestPercentage = 0.0;` should be `0.001` for this branch.

**Fault group:** D-NONSTUDENT-LOW

These five failures expose two incorrect fee calculations within the same low-deposit rule group.

### Tests D-X01–D-X04 — Invalid-Input Equivalence Classes

**Method:** `FeesCalculator.calculateDepositInterest`

**Testing technique:** Weak Robust Equivalence Class Analysis

**Test cases:**

- D-X01: Student, negative deposit amount (-$1), valid balance ($3,000).
- D-X02: Student, valid deposit amount ($100), negative balance (-$1).
- D-X03: Non-student, negative deposit amount (-$1), valid balance ($3,000).
- D-X04: Non-student, valid deposit amount ($100), negative balance (-$1).

**Expected (provisional assumption):** An `IllegalArgumentException` should be thrown when a negative monetary input is provided.

**Actual:** No exception was thrown for any of the four tests.

**Result:** All four FAIL under the provisional assumption.

**Analysis:**
These tests extend the weak robust equivalence class analysis by examining potentially invalid inputs. Each test introduces only one negative monetary value while keeping the remaining inputs valid.

Although all four tests failed, the assignment does not explicitly require `FeesCalculator` to reject negative amounts or balances by throwing `IllegalArgumentException`.

Therefore, these results cannot be classified as confirmed specification violations without further clarification from the instructor or TA.

**Fault classification:** U-FEE (assumption-dependent)

These cases are retained as provisional tests rather than confirmed faults in the ATM implementation.

## Transfer: Decision Table Analysis

### Test T-R01I — Incorrect Student Transfer Threshold and Fee

**Rule:** R01 (Student, small transfer)

**Method:** `FeesCalculator.calculateTransferFee`

**Inputs:**

- Student: true
- Transfer amount: $50
- Source balance: $1,000
- Destination balance: $500

**Expected:** $0.05 (0.1% of $50)
**Actual:** $2.50 (5% of $50)
**Result:** FAIL

**Fault localization:**
The implementation checks whether the source account balance is less than $1,000 instead of the specified $2,000. As a result, this transfer enters the incorrect balance branch and applies a 5% fee, returning $2.50 rather than the expected $0.05.

The implementation also contains incorrect fee multipliers in the student transfer branches.

**Suspected faulty conditions:**

- `if (fromAccountBalance < 1000.00)`
- `feePercentage = 0.05;`

**Fault group:** T-STUDENT-SMALL

### Tests T-R02I, T-R03I, and T-R04I — Additional Student Small-Transfer Faults

**Method:** `FeesCalculator.calculateTransferFee`

**Decision table rules:** R02, R03, R04

**Test cases:**

- T-R02I: $50 transfer, $1,000 source balance, $2,000 destination balance. Expected: $0.025; actual: $1.25.
- T-R03I: $50 transfer, $3,000 source balance, $500 destination balance. Expected: $0.025; actual: $2.50.
- T-R04I: $50 transfer, $3,000 source balance, $2,000 destination balance. Expected: $0.0125; actual: $1.25.

**Result:** All three FAIL.

**Fault localization:**
The implementation incorrectly uses a $1,000 source-balance threshold instead of the specified $2,000 for student transfers below $200.

T-R02I enters the wrong source-balance branch because its $1,000 balance is not considered less than $1,000 by the implementation.

T-R03I and T-R04I enter the appropriate balance branches but receive incorrect fee multipliers.

**Suspected faulty conditions:**

- `if (fromAccountBalance < 1000.00)`
- Incorrect fee multipliers: `0.05` and `0.025`

**Fault group:** T-STUDENT-SMALL

Together with T-R01I, these tests demonstrate incorrect threshold handling and fee calculations across all four small-transfer decision rules for students.

### Tests T-R06I, T-R07I, and T-R08I — Incorrect Student Large-Transfer Logic

**Method:** `FeesCalculator.calculateTransferFee`

**Decision table rules:** R06, R07, R08

**Test cases:**

- T-R06I: $300 transfer, $1,000 source balance, $2,000 destination balance. Expected: $0.075; actual: $3.75.
- T-R07I: $300 transfer, $3,000 source balance, $500 destination balance. Expected: $0.75; actual: $7.50.
- T-R08I: $300 transfer, $3,000 source balance, $2,000 destination balance. Expected: $0.375; actual: $3.75.

**Result:** All three FAIL.

**Fault localization:**
The implementation uses a source-balance threshold of $1,000 instead of the specified $2,000 for student transfers of $200 or more.

This causes T-R06I to enter the wrong source-balance branch. T-R07I and T-R08I reach the appropriate balance branches but receive incorrect fee multipliers.

The implementation assigns:

- R06: 0.0125 (1.25%) instead of 0.00025 (0.025%).
- R07: 0.025 (2.5%) instead of 0.0025 (0.25%).
- R08: 0.0125 (1.25%) instead of 0.00125 (0.125%).

**Suspected faulty statements:**

- `if (fromAccountBalance < 1000.00)`
- `feePercentage = 0.0125;`
- `feePercentage = 0.025;`

**Fault group:** T-STUDENT-LARGE

Together with T-R05I, these tests demonstrate incorrect threshold handling and fee multipliers across all four large-transfer decision rules for students.

### Tests T-R09I, T-R10I, T-R11I, and T-R12I — Incorrect Non-Student Small-Transfer Logic

**Method:** `FeesCalculator.calculateTransferFee`

**Decision table rules:** R09, R10, R11, R12

**Test cases:**

- T-R09I: $25 transfer, $2,000 source balance, $1,000 destination balance. Expected: $0.05; actual: $1.25.
- T-R10I: $25 transfer, $2,000 source balance, $3,000 destination balance. Expected: $0.025; actual: $1.25.
- T-R11I: $25 transfer, $5,000 source balance, $1,000 destination balance. Expected: $0.25; actual: $1.25.
- T-R12I: $25 transfer, $5,000 source balance, $3,000 destination balance. Expected: $0.125; actual: $1.25.

**Result:** All four FAIL.

**Fault localization:**
For non-student transfers below $100, the specification uses a $4,000 source-balance threshold and a $2,000 destination-balance threshold. However, the implementation uses $1,000 for both.

Consequently, all four tests enter the same high-source/high-destination branch and receive a fee of 5%, even though they represent four different decision-table rules.

The fee multipliers in this section are also incorrect. The specification requires 0.2%, 0.1%, 1%, and 0.5% across rules R09–R12, whereas the implementation uses 2%, 1%, 10%, and 5%.

**Suspected faulty conditions:**

- `if (fromAccountBalance < 1000.00)`
- `if (toAccountBalance < 1000.00)`
- Incorrect fee multipliers: `0.02`, `0.01`, `0.1`, and `0.05`

**Fault group:** T-NONSTUDENT-SMALL

These tests demonstrate how incorrect balance thresholds cause distinct decision-table rules to be treated identically, while incorrect fee multipliers produce further discrepancies.

### Tests T-R13I, T-R14I, T-R15I, and T-R16I — Incorrect Non-Student Large-Transfer Logic

**Method:** `FeesCalculator.calculateTransferFee`

**Decision table rules:** R13, R14, R15, R16

**Test cases:**

- T-R13I: $200 transfer, $1,000 source balance, $500 destination balance. Expected: $0.40; actual: $10.00.
- T-R14I: $200 transfer, $1,000 source balance, $2,000 destination balance. Expected: $0.20; actual: $11.00.
- T-R15I: $200 transfer, $3,000 source balance, $500 destination balance. Expected: $1.00; actual: $10.00.
- T-R16I: $200 transfer, $3,000 source balance, $2,000 destination balance. Expected: $0.50; actual: $11.00.

**Result:** All four FAIL.

**Fault localization:**
For non-student transfers of $100 or more, the specification uses a source-balance threshold of $2,000 and a destination-balance threshold of $1,000. The implementation incorrectly uses a source-balance threshold of $1,000.

Consequently, T-R13I and T-R14I enter the wrong source-balance branch. T-R15I and T-R16I enter the appropriate source-balance branch but still receive incorrect fee multipliers.

The implementation also assigns incorrect fee rates. In particular, the final branch uses 0.055 (5.5%) instead of the specified 0.0025 (0.25%).

**Suspected faulty conditions:**

- `if (fromAccountBalance < 1000.00)`
- Incorrect fee multipliers in the non-student large-transfer branches: `0.01`, `0.005`, `0.05`, and `0.055`

**Fault group:** T-NONSTUDENT-LARGE

These tests demonstrate incorrect threshold handling and fee calculations across all four non-student large-transfer rules.

### Tests T-X01 and T-X02 — Incorrect Student Transfer Amount Threshold

**Method:** `FeesCalculator.calculateTransferFee`

**Decision table rules:**

- T-X01: R01
- T-X02: R03

**Test cases:**

- T-X01: Student, $150 transfer, $1,500 source balance, $500 destination balance.
  - Expected: $0.15
  - Actual: $3.75
- T-X02: Student, $150 transfer, $2,500 source balance, $500 destination balance.
  - Expected: $0.075
  - Actual: $3.75

**Result:** Both FAIL.

**Fault localization:**
The specification classifies student transfers below $200 as small transfers, but the implementation checks `amount < 100.00`. Consequently, both $150 transfers incorrectly enter the large-transfer branch.

The implementation also uses an incorrect source-balance threshold of $1,000 instead of $2,000 and applies incorrect fee multipliers. Both tests therefore return $3.75 despite belonging to different decision-table rules.

**Suspected faulty conditions:**

- `if (amount < 100.00)`
- `if (fromAccountBalance < 1000.00)`
- `feePercentage = 0.025;`

**Fault group:** T-STUDENT-SMALL

These failures provide additional evidence of incorrect branching for student transfers below $200. They do not isolate the amount threshold independently because other conditions and rates are also incorrect.

### Test T-R05I — Incorrect Student Large-Transfer Fee

**Method:** `FeesCalculator.calculateTransferFee`

**Decision table rule:** R05

**Inputs:**

- Student: true
- Transfer amount: $300
- Source balance: $1,000
- Destination balance: $500

**Expected:** $0.15 (0.05% of $300)
**Actual:** $7.50 (2.5% of $300)
**Result:** FAIL

**Fault localization:**
For student transfers of $200 or more, the specification requires a source-balance threshold of $2,000. However, the implementation checks whether the source balance is below $1,000.

Because the source balance is exactly $1,000, the implementation incorrectly enters the higher-balance branch and applies a 2.5% fee instead of the required 0.05%.

**Suspected faulty conditions:**

- `if (fromAccountBalance < 1000.00)`
- `feePercentage = 0.025;`

**Fault group:** T-STUDENT-LARGE

This test reveals incorrect branch selection and an incorrect fee multiplier. The remaining large-transfer decision rules should also be checked to confirm whether they expose related faults.

### Transfer Boundary and Threshold Verification

In addition to the representative decision-table test cases, I verified 16 boundary representatives (T-R01B through T-R16B) and seven supplementary threshold tests (T-X03 through T-X09).

All 23 tests failed, and their observed results were consistent with the implementation's incorrect conditions and fee multipliers.

The boundary tests provided additional evidence of the four previously identified fault groups:

- **T-STUDENT-SMALL:** R01B–R04B confirm failures involving the incorrect student amount and source-balance thresholds and fee calculations.
- **T-STUDENT-LARGE:** R05B–R08B and X03–X04 confirm incorrect source-balance threshold handling and fee multipliers for student transfers of $200 or more.
- **T-NONSTUDENT-SMALL:** R09B–R12B and X05–X06 confirm incorrect source/destination balance thresholds and fee multipliers for non-student transfers below $100.
- **T-NONSTUDENT-LARGE:** R13B–R16B and X07–X09 confirm incorrect source-balance threshold handling and fee multipliers for non-student transfers of $100 or more.

**Overall transfer results:**

- Total tests: 41
- Passed: 0
- Failed: 41
- Errors: 0

The results demonstrate that the implementation does not correctly calculate transfer fees according to the 16 decision-table rules. Testing values at and around decision boundaries also provides evidence that several incorrect conditions cause transactions to enter the wrong branches.

All identified faults concern `bank.utils.FeesCalculator.calculateTransferFee`. No production code was modified.
