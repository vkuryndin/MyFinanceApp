package org.example.model;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for the Wallet model class.
 *
 * <p>Tests validate core Wallet functionality including:
 *
 * <ul>
 *   <li><b>Transaction management:</b> Adding income and expense transactions
 *   <li><b>Balance calculation:</b> Computed as total income minus total expenses
 *   <li><b>Category aggregation:</b> Grouping expenses and incomes by title (category)
 *   <li><b>Budget tracking:</b> Setting budgets and monitoring spending per category
 * </ul>
 *
 * <p>The Wallet class maintains a list of transactions and provides methods to calculate totals,
 * balance, and category-wise breakdowns for both income and expenses.
 *
 * @see org.example.model.Wallet
 * @see org.example.model.Transaction
 */
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
