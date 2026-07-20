package com.spearforge.sBank.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class BankTest {
    @Test
    void balanceKeepsCentsWithoutLocaleFormatting() {
        Bank bank = new Bank();
        bank.setBalance(1_234_567.899D);

        assertEquals(1_234_567.90D, bank.getBalance());
    }

    @Test
    void balanceRejectsInvalidCurrency() {
        Bank bank = new Bank();
        assertThrows(IllegalArgumentException.class, () -> bank.setBalance(Double.NaN));
        assertThrows(IllegalArgumentException.class, () -> bank.setBalance(-1.0D));
    }
}
