package bank.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvFileSource;

/** Assignment 1 Withdrawal: robust worst-case BVA; see docs/withdrawal-bva.md. */
class WithdrawalFeeTest {
    private final FeesCalculator calculator = new FeesCalculator();

    @ParameterizedTest(name = "{0}")
    @CsvFileSource(resources = "/assignment1/withdrawal.csv", numLinesToSkip = 1)
    void withdrawalFee(String id, double amount, double balance, boolean student,
            int day, Double expectedFee) {
        if (expectedFee == null) {
            // Exercise the input, then report it as skipped because no oracle is specified.
            String observed = observe(amount, balance, student, day);
            assumeTrue(false, id + ": no specified expected outcome; observed " + observed);
        } else {
            assertEquals(expectedFee.doubleValue(),
                calculator.calculateWithdrawalFee(amount, balance, student, day), 1e-9, id);
        }
    }

    private String observe(double amount, double balance, boolean student, int day) {
        try {
            return "fee=" + calculator.calculateWithdrawalFee(amount, balance, student, day);
        } catch (RuntimeException exception) {
            return exception.getClass().getSimpleName() + ": " + exception.getMessage();
        }
    }
}
