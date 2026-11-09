package bank.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvFileSource;

/** Assignment 1 Deposit: weak robust equivalence classes; see docs/assignment1.md. */
class DepositFeeTest {
    private final FeesCalculator calculator = new FeesCalculator();

    @ParameterizedTest(name = "{0}")
    @CsvFileSource(resources = "/assignment1/deposit.csv", numLinesToSkip = 1)
    void deposit(String id, double amount, double balance, boolean student, Double expectedFee) {
        if (expectedFee == null) {
            assumeTrue(false, id + ": no specified expected outcome; observed " + observe(amount, balance, student));
        } else {
            assertEquals(expectedFee.doubleValue(), calculator.calculateDepositInterest(amount, balance, student),
                1e-9, id);
        }
    }

    private String observe(double amount, double balance, boolean student) {
        try {
            return "fee=" + calculator.calculateDepositInterest(amount, balance, student);
        } catch (RuntimeException exception) {
            return exception.getClass().getSimpleName() + ": " + exception.getMessage();
        }
    }
}
