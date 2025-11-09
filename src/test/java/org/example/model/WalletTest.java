package org.example.model;

import static org.junit.jupiter.api.Assertions.*;

import java.time.LocalDate;
import java.util.Map;
import java.util.Set;
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
 * <p>Дополнено тестами для расширенных функций: * - renameCategory, updateBudgetLimit, removeBudget
 * * - sumIncome/sumExpense с диапазоном дат и фильтром категорий * -
 * incomesByCategory/expensesByCategory с диапазоном дат и фильтром категорий * - нормализация
 * набора категорий (косвенно через публичные методы)
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

  // -------------------- НОВОЕ: бюджеты --------------------

  @Test
  @DisplayName("updateBudgetLimit updates existing category, removeBudget deletes it")
  void updateAndRemoveBudget() {
    Wallet w = new Wallet();
    w.setBudget("food", 200.0);
    w.setBudget("travel", 500.0);

    // update the existing
    assertTrue(w.updateBudgetLimit("food", 300.0));
    assertEquals(300.0, w.getBudgets().get("food"), 1e-9);

    // update the existing
    assertFalse(w.updateBudgetLimit("entertainment", 100.0));

    // delete the existing
    assertTrue(w.removeBudget("travel"));
    assertFalse(w.getBudgets().containsKey("travel"));

    // delete the non existent
    assertFalse(w.removeBudget("unknown"));
  }

  @Test
  @DisplayName("renameCategory moves budget limit and merges spent amounts")
  void renameCategory_movesBudgetAndSpent() {
    Wallet w = new Wallet();
    // limit and expenses by по oldName
    w.setBudget("food", 200.0);
    w.addTransaction(new Transaction(50.0, "food", Transaction.Type.EXPENSE, "2025-01-10"));
    w.addTransaction(new Transaction(30.0, "food", Transaction.Type.EXPENSE, "2025-01-11"));

    assertEquals(80.0, w.getSpentByCategory("food"), 1e-9);

    // the target category which already ahs a limit checking merge
    w.setBudget("groceries", 500.0);
    w.addTransaction(new Transaction(20.0, "groceries", Transaction.Type.EXPENSE, "2025-01-09"));

    boolean changed = w.renameCategory("food", "groceries");
    assertTrue(changed, "Expected rename to change something");

    // Лимит в целевой перезаписывается лимитом источника (как сказано в коде)
    assertEquals(200.0, w.getBudgets().get("groceries"), 1e-9);
    assertFalse(w.getBudgets().containsKey("food"));

    // Траты суммируются
    assertEquals(100.0, w.getSpentByCategory("groceries"), 1e-9); // 80 + 20
    assertEquals(0.0, w.getSpentByCategory("food"), 1e-9);
  }

  @Test
  @DisplayName("renameCategory invalid inputs return false and do nothing")
  void renameCategory_invalidInputs() {
    Wallet w = new Wallet();
    w.setBudget("x", 10.0);
    assertFalse(w.renameCategory(null, "y"));
    assertFalse(w.renameCategory("x", null));
    assertFalse(w.renameCategory("", "y"));
    assertFalse(w.renameCategory("x", ""));
    assertFalse(w.renameCategory("same", "same"));
    // убеждаемся, что ничего не сломали
    assertTrue(w.getBudgets().containsKey("x"));
  }

  // -------------------- НОВОЕ: периоды + категории --------------------

  @Test
  @DisplayName("sumIncome / sumExpense respect date range and category filter")
  void sumIncomeExpense_rangeAndCategories() {
    Wallet w = new Wallet();
    // доходы
    w.addTransaction(new Transaction(1000.0, "salary", Transaction.Type.INCOME, "2025-01-01"));
    w.addTransaction(new Transaction(200.0, "gift", Transaction.Type.INCOME, "2025-02-05"));
    // расходы
    w.addTransaction(new Transaction(100.0, "food", Transaction.Type.EXPENSE, "2025-01-15"));
    w.addTransaction(new Transaction(50.0, "food", Transaction.Type.EXPENSE, "2025-02-01"));
    w.addTransaction(new Transaction(70.0, "transport", Transaction.Type.EXPENSE, "2025-01-20"));

    LocalDate from = LocalDate.of(2025, 1, 10);
    LocalDate to = LocalDate.of(2025, 1, 31);

    // Только январь
    assertEquals(0.0, w.sumIncome(from, to, java.util.Set.of()), 1e-9);
    assertEquals(
        170.0, w.sumExpense(from, to, java.util.Set.of()), 1e-9); // 100 food + 70 transport

    // Только категория food в январе
    assertEquals(100.0, w.sumExpense(from, to, java.util.Set.of("food")), 1e-9);

    // Включая февраль и без фильтра категорий
    assertEquals(
        220.0,
        w.sumExpense(LocalDate.of(2025, 1, 1), LocalDate.of(2025, 2, 28), java.util.Set.of()),
        1e-9); // 100 + 50 + 70

    // Доходы в том же периоде (оба дохода попадают)
    assertEquals(
        1200.0,
        w.sumIncome(LocalDate.of(2025, 1, 1), LocalDate.of(2025, 2, 28), java.util.Set.of()),
        1e-9);
  }

  @Test
  @DisplayName("expensesByCategory(range, cats) aggregates only selected categories in range")
  void expensesByCategory_rangeAndCats() {
    Wallet w = new Wallet();
    w.addTransaction(new Transaction(10.0, "food", Transaction.Type.EXPENSE, "2025-01-10"));
    w.addTransaction(new Transaction(20.0, "travel", Transaction.Type.EXPENSE, "2025-01-12"));
    w.addTransaction(new Transaction(5.0, "food", Transaction.Type.EXPENSE, "2025-02-01"));

    Map<String, Double> jan =
        w.expensesByCategory(
            LocalDate.of(2025, 1, 1), LocalDate.of(2025, 1, 31), Set.of("food", "travel"));
    assertEquals(2, jan.size());
    assertEquals(10.0, jan.get("food"), 1e-9);
    assertEquals(20.0, jan.get("travel"), 1e-9);

    Map<String, Double> onlyFoodJan =
        w.expensesByCategory(LocalDate.of(2025, 1, 1), LocalDate.of(2025, 1, 31), Set.of("food"));
    assertEquals(1, onlyFoodJan.size());
    assertEquals(10.0, onlyFoodJan.get("food"), 1e-9);
  }

  @Test
  @DisplayName(
      "incomesByCategory(range, cats) aggregates incomes in range and filters by categories")
  void incomesByCategory_rangeAndCats() {
    Wallet w = new Wallet();
    w.addTransaction(new Transaction(100.0, "salary", Transaction.Type.INCOME, "2025-01-05"));
    w.addTransaction(new Transaction(50.0, "bonus", Transaction.Type.INCOME, "2025-01-20"));
    w.addTransaction(new Transaction(200.0, "salary", Transaction.Type.INCOME, "2025-02-01"));

    Map<String, Double> janSalary =
        w.incomesByCategory(LocalDate.of(2025, 1, 1), LocalDate.of(2025, 1, 31), Set.of("salary"));
    assertEquals(1, janSalary.size());
    assertEquals(100.0, janSalary.get("salary"), 1e-9);

    Map<String, Double> janAll =
        w.incomesByCategory(LocalDate.of(2025, 1, 1), LocalDate.of(2025, 1, 31), Set.of());
    assertEquals(2, janAll.size());
    assertEquals(100.0, janAll.get("salary"), 1e-9);
    assertEquals(50.0, janAll.get("bonus"), 1e-9);
  }

  @Test
  @DisplayName("Category set normalization works via public filters (trim, skip blanks/nulls)")
  void categoryNormalization_viaPublicAPI() {
    Wallet w = new Wallet();
    w.addTransaction(new Transaction(30.0, "food", Transaction.Type.EXPENSE, "2025-01-10"));
    w.addTransaction(new Transaction(40.0, "travel", Transaction.Type.EXPENSE, "2025-01-11"));
    w.addTransaction(new Transaction(50.0, "other", Transaction.Type.EXPENSE, "2025-01-12"));

    // передаём «грязный» набор категорий с пробелами и пустышками
    Set<String> dirty =
        new java.util.HashSet<>(java.util.Arrays.asList(" food ", " ", "", null, "travel"));

    Map<String, Double> m =
        w.expensesByCategory(LocalDate.of(2025, 1, 1), LocalDate.of(2025, 1, 31), dirty);

    // ожидаем, что нормализация оставила только {"food","travel"}
    assertEquals(2, m.size());
    assertEquals(30.0, m.get("food"), 1e-9);
    assertEquals(40.0, m.get("travel"), 1e-9);
    assertFalse(m.containsKey("other"));
  }
}
