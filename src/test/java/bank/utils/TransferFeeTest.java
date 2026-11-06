package bank.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvFileSource;

/** Assignment 1 Transfer: all 16 decision rules, plus threshold probes. */
class TransferFeeTest {
    private final FeesCalculator calculator = new FeesCalculator();

    @ParameterizedTest(name = "{0}")
    @CsvFileSource(resources = "/assignment1/transfer.csv", numLinesToSkip = 1)
    void transfer(String id, double amount, double from, double to, boolean student, double expected) {
        assertEquals(expected, calculator.calculateTransferFee(amount, from, to, student), 1e-9, id);
    }
}
