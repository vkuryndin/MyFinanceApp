package org.example.model;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

public class WalletTest {

    @Test
    @DisplayName("INCOME and EXPENSE are correctly summed; balance = income - expense")
    void incomeExpenseAndBalance() {
        Wallet w = new Wallet();
        w.addTransaction(1000.0, "salary", Transaction.Type.INCOME);
        w.addTransaction(200.0, "food", Transaction.Type.EXPENSE);
        w.addTransaction(300.0, "books", Transaction.Type.EXPENSE);

        assertEquals(1000.0, w.sumIncome(), 1e-9);
        assertEquals(500.0, w.sumExpense(), 1e-9);
        assertEquals(500.0, w.getBalance(), 1e-9);
        assertEquals(3, w.transactions.size());
    }

    @Test
    @DisplayName("expensesByCategory groups expenses by title (category)")
    void expensesByCategory() {
        Wallet w = new Wallet();
        w.addTransaction(100, "food", Transaction.Type.EXPENSE);
        w.addTransaction(50, "food", Transaction.Type.EXPENSE);
        w.addTransaction(30, "transport", Transaction.Type.EXPENSE);
        w.addTransaction(200, "salary", Transaction.Type.INCOME);

        Map<String, Double> m = w.expensesByCategory();
        assertEquals(2, m.size());
        assertEquals(150.0, m.get("food"), 1e-9);
        assertEquals(30.0, m.get("transport"), 1e-9);
    }
}
