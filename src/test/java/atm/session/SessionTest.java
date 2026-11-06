package atm.session;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

import atm.ATM;
import atm.dispatcher.JavaDispatcher;
import atm.dispatcher.MessageDispatcher;
import atm.exceptions.InvalidAmountException;
import atm.exceptions.InvalidPinFormatException;
import atm.session.transactions.ATMWithdrawal;
import atm.ui.panels.MainPanel;
import atm.utils.CredentialsCheck;
import atm.utils.FormatChecker;
import bank.BankFacade;
import bank.db.DBHandler;
import bank.transactions.BankWithdrawal;
import bank.transactions.utils.AccountType;
import bank.utils.FeesCalculator;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvFileSource;

/** Assignment 1 boundary tests for withdrawal amount and PIN format. */
class SessionTest {
    private static final String CARD = "4111111111111111";
    private static final String USER = "assignment1";
    private static final double AVAILABLE_BALANCE = 5000.0;
    private static final String VALID_PIN = "12345";

    private Session session(MessageDispatcher dispatcher) {
        Session session = new Session(mock(MainPanel.class), dispatcher);
        session.insertCard();
        session.addCard(CARD);
        return session;
    }

    @ParameterizedTest(name = "{0}")
    @CsvFileSource(resources = "/assignment1/session-pin.csv", numLinesToSkip = 1, emptyValue = "")
    void sessionPin(String id, String pin, boolean valid) {
        Session session = session(mock(MessageDispatcher.class));
        if (valid) {
            assertDoesNotThrow(() -> session.addPin(pin.toCharArray()), id);
        } else {
            assertThrows(InvalidPinFormatException.class, () -> session.addPin(pin.toCharArray()), id);
        }
    }

    // Supplemental check of the caller that performs format validation before entering Session.
    @ParameterizedTest(name = "{0}")
    @CsvFileSource(resources = "/assignment1/atm-pin.csv", numLinesToSkip = 1, emptyValue = "")
    void atmPin(String id, String pin, boolean valid) throws Exception {
        MessageDispatcher dispatcher = mock(MessageDispatcher.class);
        doReturn(true).when(dispatcher).checkCredentials(any(), any());
        ATM atm = new ATM(new FormatChecker(), new CredentialsCheck(dispatcher), dispatcher);
        atm.setMainPanel(mock(MainPanel.class));
        atm.createSession();
        atm.checkCardNumber(CARD);
        if (valid) {
            assertDoesNotThrow(() -> atm.checkPin(pin.toCharArray()), id);
        } else {
            assertThrows(InvalidPinFormatException.class, () -> atm.checkPin(pin.toCharArray()), id);
        }
    }

    @ParameterizedTest(name = "{0}")
    @CsvFileSource(resources = "/assignment1/session-amount.csv", numLinesToSkip = 1)
    void withdrawalAmount(String id, int amount, boolean valid) throws Exception {
        DBHandler db = mock(DBHandler.class);
        doReturn(USER).when(db).getCardOwner(CARD);
        doReturn(VALID_PIN.toCharArray()).when(db).getPIN(USER);
        doReturn(AVAILABLE_BALANCE).when(db).getBalance(USER, AccountType.Chequing);
        doReturn(true).when(db).isStudent(USER);
        // Fees are isolated so a fee defect cannot mask an amount-validation failure.
        BankWithdrawal withdrawal = new BankWithdrawal(mock(FeesCalculator.class), db);
        BankFacade bank = new BankFacade(db, withdrawal, null, null);
        Session session = session(new JavaDispatcher(bank));
        session.addPin(VALID_PIN.toCharArray());
        session.setTransaction(new ATMWithdrawal()); // transaction choice 1
        session.setFromAccount(AccountType.Chequing);
        if (valid) {
            assertDoesNotThrow(() -> session.setAmount(amount), id);
        } else {
            assertThrows(InvalidAmountException.class, () -> session.setAmount(amount), id);
        }
    }
}
