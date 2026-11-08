package bank.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvFileSource;

/** Assignment 1 Deposit: weak robust equivalence classes; see docs/assignment1.md. */
class DepositFeeTest {
    private final FeesCalculator calculator = new FeesCalculator();

    @ParameterizedTest(name = "{0}")
    @CsvFileSource(resources = "/assignment1/deposit.csv", numLinesToSkip = 1)
    void deposit(String id, double amount, double balance, boolean student, double expected,
            boolean assumedRejection) {
        if (assumedRejection) {
            assertThrows(IllegalArgumentException.class,
                () -> calculator.calculateDepositInterest(amount, balance, student),
                id + ": provisional invalid-input assumption; not a specified exception contract");
        } else {
            assertEquals(expected, calculator.calculateDepositInterest(amount, balance, student), 1e-9, id);
        }
    }
}
